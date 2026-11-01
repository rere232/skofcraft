package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.registry.SkofcraftEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * A tungsten rod dropped by a strike satellite. It waits high above the target for {@link #WARNING_TICKS} while a
 * targeting beam marks the spot, then falls at {@link #SPEED} blocks per tick and hits like a bomb.
 */
public class OrbitalRod extends Entity {
    public static final int WARNING_TICKS = 60;
    private static final double SPEED = 6.0D;
    private static final double SPAWN_ABOVE_BUILD_LIMIT = 40.0D;
    private static final float IMPACT_POWER = 10.0F;
    private static final float CRATER_POWER = 7.0F;
    private static final double CRATER_DEPTH = 3.0D;
    private static final double BEAM_HEIGHT = 80.0D;
    private static final double BEAM_VIEW_RANGE = 512.0D;
    private static final ParticleOptions TARGET_DOT = new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 2.0F);

    private Vec3 target = Vec3.ZERO;
    private int warning = WARNING_TICKS;
    @Nullable
    private UUID caller;

    public OrbitalRod(EntityType<? extends OrbitalRod> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** Sends a rod down on {@code target}, called in by {@code caller}. */
    public static void call(ServerLevel level, Vec3 target, Player caller) {
        OrbitalRod rod = new OrbitalRod(SkofcraftEntities.ORBITAL_ROD.get(), level);
        rod.setPos(target.x, Math.max(target.y + BEAM_HEIGHT, level.getMaxBuildHeight() + SPAWN_ABOVE_BUILD_LIMIT), target.z);
        rod.target = target;
        rod.caller = caller.getUUID();
        level.addFreshEntity(rod);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (getY() < yo) {
                for (int i = 0; i < 6; i++) {
                    double back = random.nextDouble() * SPEED;
                    level().addAlwaysVisibleParticle(i % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.LARGE_SMOKE, true,
                            getX() + (random.nextDouble() - 0.5D) * 0.4D, getY() + 3.0D + back, getZ() + (random.nextDouble() - 0.5D) * 0.4D,
                            0.0D, 0.0D, 0.0D);
                }
            }
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (warning > 0) {
            warning--;
            if (warning % 2 == 0) {
                beam(level);
            }
            if (warning % 20 == 0) {
                level.playSound(null, target.x, target.y, target.z, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.HOSTILE, 3.0F, 2.0F);
            }
            if (warning == 0) {
                level.playSound(null, target.x, target.y, target.z, SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 6.0F, 1.4F);
            }
            return;
        }

        Vec3 from = position();
        Vec3 to = from.add(0.0D, -SPEED, 0.0D);
        setDeltaMovement(0.0D, -SPEED, 0.0D);
        if (to.y < level.getMinBuildHeight()) {
            discard();
            return;
        }
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
        Vec3 impact = hit.getType() == HitResult.Type.MISS ? null : hit.getLocation();
        for (Entity victim : level.getEntities(this, new AABB(from, to).inflate(0.6D))) {
            if (victim.isAlive() && victim.isPickable()) {
                Vec3 at = victim.position();
                if (impact == null || at.y > impact.y) {
                    impact = new Vec3(getX(), at.y, getZ());
                }
            }
        }
        if (impact != null) {
            impact(level, impact);
        } else {
            setPos(to);
        }
    }

    /** Red targeting beam from the sky to the target, sent with the long-distance flag so it shows from far away. */
    private void beam(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(target) > BEAM_VIEW_RANGE * BEAM_VIEW_RANGE) {
                continue;
            }
            for (int i = 0; i < 16; i++) {
                double y = target.y + random.nextDouble() * BEAM_HEIGHT;
                level.sendParticles(player, ParticleTypes.END_ROD, true, target.x, y, target.z, 1, 0.05D, 0.0D, 0.05D, 0.0D);
            }
            for (int i = 0; i < 12; i++) {
                double angle = i * Math.PI * 2.0D / 12.0D + warning * 0.2D;
                double radius = 1.0D + warning / (double) WARNING_TICKS * 3.0D;
                level.sendParticles(player, TARGET_DOT, true, target.x + Math.cos(angle) * radius, target.y + 0.2D,
                        target.z + Math.sin(angle) * radius, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private void impact(ServerLevel level, Vec3 at) {
        level.explode(this, at.x, at.y, at.z, IMPACT_POWER, true, Level.ExplosionInteraction.BLOCK);
        level.explode(this, at.x, at.y - CRATER_DEPTH, at.z, CRATER_POWER, false, Level.ExplosionInteraction.BLOCK);
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(at) < BEAM_VIEW_RANGE * BEAM_VIEW_RANGE) {
                level.sendParticles(player, ParticleTypes.EXPLOSION_EMITTER, true, at.x, at.y + 1.0D, at.z, 8, 3.0D, 2.0D, 3.0D, 0.0D);
                level.sendParticles(player, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, at.x, at.y + 2.0D, at.z, 60, 4.0D, 3.0D, 4.0D, 0.05D);
            }
        }
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 12.0F, 0.5F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 8.0F, 0.8F);
        if (caller != null) {
            Player player = level.getPlayerByUUID(caller);
            if (player != null) {
                SkofcraftAdvancements.award(player, SkofcraftAdvancements.ORBITAL_STRIKE);
            }
        }
        discard();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < BEAM_VIEW_RANGE * BEAM_VIEW_RANGE;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        target = new Vec3(tag.getDouble("TargetX"), tag.getDouble("TargetY"), tag.getDouble("TargetZ"));
        warning = tag.getInt("Warning");
        caller = tag.hasUUID("Caller") ? tag.getUUID("Caller") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("TargetX", target.x);
        tag.putDouble("TargetY", target.y);
        tag.putDouble("TargetZ", target.z);
        tag.putInt("Warning", warning);
        if (caller != null) {
            tag.putUUID("Caller", caller);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
