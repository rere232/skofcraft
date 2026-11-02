package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.registry.SkofcraftEntities;
import ch.rere232.skofcraft.registry.SkofcraftSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;

/**
 * A heavyweight torpedo. It runs straight for a moment, then homes in on whatever is in the water ahead of it, boats and
 * submarines first. It can't leave the water. A torpedo that hits something does real damage on top of the blast, and
 * since water stops an ordinary explosion from breaking anything, it also punches a flooded hole in whatever it hit.
 */
public class Torpedo extends Projectile {
    private static final double SPEED = 0.9D;
    private static final int ARMING_TICKS = 15;
    private static final int LIFETIME = 20 * 30;
    private static final double SEEKER_RANGE = 48.0D;
    /** Sixty degrees off the nose, wide enough to find a ship on the surface from a dive. */
    private static final double SEEKER_CONE = 0.5D;
    private static final double LOCK_RANGE = 96.0D;
    private static final double TURN_RATE = 0.1D;
    private static final int SEEK_INTERVAL = 5;
    private static final float DIRECT_DAMAGE = 40.0F;
    private static final float BLAST_POWER = 6.0F;
    private static final int HOLE_RADIUS = 4;
    private static final float UNBREAKABLE_RESISTANCE = 1200.0F;

    /** The boat that fired it, which it won't hit. */
    private int firedFrom = -1;
    @Nullable
    private Entity target;

    public Torpedo(EntityType<? extends Torpedo> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public Torpedo(Level level, LivingEntity owner, NuclearSubmarine boat, Vec3 position, Vec3 direction) {
        this(SkofcraftEntities.TORPEDO.get(), level);
        setOwner(owner);
        firedFrom = boat.getId();
        setPos(position.x, position.y, position.z);
        setDeltaMovement(direction.normalize().scale(SPEED));
        updateRotation();
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();
        boolean wet = level().getFluidState(blockPosition()).is(FluidTags.WATER);
        if (level() instanceof ServerLevel) {
            if (tickCount > LIFETIME) {
                detonate(null);
                return;
            }
            if (wet && tickCount > ARMING_TICKS) {
                motion = steer(motion);
            }
        }
        if (wet) {
            // Runs along the surface rather than breaching it
            if (motion.y > 0.0D && !level().getFluidState(BlockPos.containing(getX(), getY() + 0.6D, getZ())).is(FluidTags.WATER)) {
                motion = new Vec3(motion.x, 0.0D, motion.z).normalize().scale(SPEED);
            }
        } else {
            motion = motion.scale(0.98D).add(0.0D, -0.05D, 0.0D);
        }
        setDeltaMovement(motion);

        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            if (!level().isClientSide) {
                detonate(hit);
            }
            return;
        }
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        updateRotation();
        if (level().isClientSide && wet) {
            level().addParticle(ParticleTypes.BUBBLE, getX(), getY(), getZ(), 0.0D, 0.05D, 0.0D);
        }
    }

    private Vec3 steer(Vec3 motion) {
        // A lock is kept even if the target ends up behind: it comes around for another pass
        if (target != null && (!target.isAlive() || target.distanceToSqr(this) > LOCK_RANGE * LOCK_RANGE)) {
            target = null;
        }
        if (target == null && tickCount % SEEK_INTERVAL == 0) {
            target = seek(motion);
        }
        if (target == null) {
            return motion;
        }
        Vec3 aim = target.position().add(0.0D, target.getBbHeight() / 2.0D, 0.0D).subtract(position()).normalize();
        return motion.normalize().add(aim.subtract(motion.normalize()).scale(TURN_RATE)).normalize().scale(SPEED);
    }

    @Nullable
    private Entity seek(Vec3 motion) {
        Vec3 heading = motion.normalize();
        Optional<Entity> best = level().getEntitiesOfClass(Entity.class, getBoundingBox().inflate(SEEKER_RANGE), entity -> isPrey(entity, heading))
                .stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(this) / priority(entity)));
        return best.orElse(null);
    }

    private boolean isPrey(Entity entity, Vec3 heading) {
        if (entity == this || !entity.isAlive() || entity.getId() == firedFrom || entity == getOwner() || entity.isSpectator() || entity instanceof Torpedo) {
            return false;
        }
        if (entity.getVehicle() != null && entity.getVehicle().getId() == firedFrom) {
            return false;
        }
        if (!(entity instanceof NuclearSubmarine || entity instanceof Boat || entity instanceof AircraftCarrier
                || (entity instanceof LivingEntity && entity.isInWater()))) {
            return false;
        }
        return entity.position().subtract(position()).normalize().dot(heading) > SEEKER_CONE;
    }

    private static double priority(Entity entity) {
        if (entity instanceof NuclearSubmarine || entity instanceof AircraftCarrier) {
            return 16.0D;
        }
        return entity instanceof Boat || entity instanceof Player ? 4.0D : 1.0D;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (tickCount < ARMING_TICKS || !super.canHitEntity(entity) || entity.getId() == firedFrom) {
            return false;
        }
        return !(entity instanceof SubmarinePart part && part.getParent().getId() == firedFrom);
    }

    private void detonate(@Nullable HitResult hit) {
        ServerLevel level = (ServerLevel) level();
        Entity owner = getOwner();
        if (hit instanceof EntityHitResult entityHit) {
            entityHit.getEntity().hurt(damageSources().explosion(this, owner), DIRECT_DAMAGE);
            if (owner != null) {
                SkofcraftAdvancements.award(owner, SkofcraftAdvancements.FISH_IN_THE_WATER);
            }
        }
        double x = getX();
        double y = getY();
        double z = getZ();
        discard();
        level.explode(this, x, y, z, BLAST_POWER, Level.ExplosionInteraction.NONE);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 2, 1.0D, 1.0D, 1.0D, 0.0D);
        level.sendParticles(ParticleTypes.BUBBLE, x, y, z, 120, 3.0D, 3.0D, 3.0D, 0.2D);
        level.playSound(null, x, y, z, SkofcraftSounds.EXPLOSION.get(), SoundSource.NEUTRAL, 8.0F, 0.7F);
        level.playSound(null, x, y, z, SkofcraftSounds.HULL_CLANG.get(), SoundSource.NEUTRAL, 4.0F, 0.5F);
        if (level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            punchHole(level, BlockPos.containing(x, y, z));
        }
    }

    /** Everything breakable in the radius goes, and the water comes in as far up as it already reaches. */
    private static void punchHole(ServerLevel level, BlockPos center) {
        int waterTop = center.getY();
        while (level.getFluidState(new BlockPos(center.getX(), waterTop + 1, center.getZ())).is(FluidTags.WATER)) {
            waterTop++;
        }
        boolean underwater = level.getFluidState(center).is(FluidTags.WATER);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-HOLE_RADIUS, -HOLE_RADIUS, -HOLE_RADIUS), center.offset(HOLE_RADIUS, HOLE_RADIUS, HOLE_RADIUS))) {
            if (pos.distSqr(center) > HOLE_RADIUS * HOLE_RADIUS) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty() || state.getBlock().getExplosionResistance() >= UNBREAKABLE_RESISTANCE) {
                continue;
            }
            level.destroyBlock(pos, false);
            if (underwater && pos.getY() <= waterTop) {
                level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
            }
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256.0D * 256.0D;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
