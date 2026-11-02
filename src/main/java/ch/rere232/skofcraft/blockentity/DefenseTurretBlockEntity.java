package ch.rere232.skofcraft.blockentity;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.block.DefenseTurretBlock;
import ch.rere232.skofcraft.entity.ApacheBullet;
import ch.rere232.skofcraft.entity.ApacheRocket;
import ch.rere232.skofcraft.entity.PilotedAircraft;
import ch.rere232.skofcraft.registry.SkofcraftEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Brain of a defence turret. The server picks the nearest target it may engage and can see, leads it and fires;
 * only the target's entity id is synced, and the client turns the head toward that entity on its own.
 */
public class DefenseTurretBlockEntity extends BlockEntity {
    /** Height of the gun's pivot above the block's base, matching the renderer. */
    public static final double PIVOT_HEIGHT = 1.0D;
    private static final int SCAN_INTERVAL = 10;
    private static final int RECHECK_INTERVAL = 5;
    /** Ticks a new target is tracked before the first shot, while the head swings round. */
    private static final int SPIN_UP = 8;
    private static final float TURN_SPEED = 14.0F;

    @Nullable
    private UUID owner;
    private boolean engageAllThreats;

    // Server
    @Nullable
    private Entity target;
    private Vec3 targetLastPos = Vec3.ZERO;
    private Vec3 targetVelocity = Vec3.ZERO;
    private int trackTicks;
    private int cooldown;
    private int ticks;
    private boolean leftTube;

    // Synced to the client; the head is animated from it
    private int targetId = -1;
    public float yaw;
    public float pitch;
    public float spin;
    public float prevYaw;
    public float prevPitch;
    public float prevSpin;

    public DefenseTurretBlockEntity(BlockPos pos, BlockState state) {
        super(SkofcraftBlockEntities.DEFENSE_TURRET.get(), pos, state);
    }

    public DefenseTurretBlock.Weapon weapon() {
        return getBlockState().getBlock() instanceof DefenseTurretBlock block ? block.weapon : DefenseTurretBlock.Weapon.CIWS;
    }

    // ---------------------------------------------------------------- server

    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ticks++;
        if (target != null && (ticks % RECHECK_INTERVAL == 0 && !canEngage(target))) {
            setTarget(null);
        }
        if (target == null && ticks % SCAN_INTERVAL == 0) {
            setTarget(findTarget());
        }
        if (cooldown > 0) {
            cooldown--;
        }
        if (target == null) {
            return;
        }
        Vec3 position = target.position();
        targetVelocity = trackTicks == 0 ? Vec3.ZERO : position.subtract(targetLastPos);
        targetLastPos = position;
        trackTicks++;
        if (trackTicks >= SPIN_UP && cooldown <= 0) {
            fire(serverLevel, target);
            cooldown = weapon().cooldown;
        }
    }

    private void setTarget(@Nullable Entity newTarget) {
        target = newTarget;
        trackTicks = 0;
        int id = newTarget == null ? -1 : newTarget.getId();
        if (id != targetId) {
            targetId = id;
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    private Vec3 pivot() {
        return new Vec3(worldPosition.getX() + 0.5D, worldPosition.getY() + PIVOT_HEIGHT, worldPosition.getZ() + 0.5D);
    }

    @Nullable
    private Entity findTarget() {
        if (level == null) {
            return null;
        }
        Vec3 pivot = pivot();
        double range = weapon().range;
        List<Entity> candidates = level.getEntities((Entity) null, new AABB(pivot, pivot).inflate(range), this::mayEngage);
        candidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(pivot)));
        for (Entity candidate : candidates) {
            if (inRange(candidate) && canSee(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean canEngage(Entity entity) {
        return mayEngage(entity) && inRange(entity) && canSee(entity);
    }

    private boolean inRange(Entity entity) {
        double range = weapon().range;
        return entity.getBoundingBox().getCenter().distanceToSqr(pivot()) <= range * range;
    }

    /** Whether this turret is allowed to shoot at the entity at all, whatever the range. */
    private boolean mayEngage(Entity entity) {
        if (!entity.isAlive() || entity.isRemoved() || entity.isSpectator()) {
            return false;
        }
        boolean airborne;
        if (entity instanceof PilotedAircraft) {
            Entity pilot = entity.getControllingPassenger();
            if (!engageAllThreats || pilot == null || isFriendly(pilot)) {
                return false;
            }
            airborne = !entity.onGround();
        } else if (entity instanceof Player player) {
            if (!engageAllThreats || player.isCreative() || isFriendly(player) || player.getVehicle() instanceof PilotedAircraft) {
                return false;
            }
            airborne = player.isFallFlying();
        } else if (entity instanceof Enemy && entity instanceof LivingEntity) {
            airborne = entity instanceof FlyingMob || entity instanceof EnderDragon || entity instanceof WitherBoss
                    || entity instanceof Blaze || entity instanceof Vex;
        } else {
            return false;
        }
        return weapon() == DefenseTurretBlock.Weapon.CIWS || airborne;
    }

    private boolean isFriendly(Entity entity) {
        if (owner == null || level == null) {
            return false;
        }
        if (entity.getUUID().equals(owner)) {
            return true;
        }
        Player ownerPlayer = level.getPlayerByUUID(owner);
        return ownerPlayer != null && entity.isAlliedTo(ownerPlayer);
    }

    private boolean canSee(Entity entity) {
        if (level == null) {
            return false;
        }
        Vec3 to = entity.getBoundingBox().getCenter();
        Vec3 pivot = pivot();
        Vec3 from = pivot.add(to.subtract(pivot).normalize().scale(0.9D));
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null)).getType() == HitResult.Type.MISS;
    }

    private void fire(ServerLevel level, Entity target) {
        DefenseTurretBlock.Weapon weapon = weapon();
        Vec3 pivot = pivot();
        Vec3 center = target.getBoundingBox().getCenter();
        // Lead the target by where it will be when the shot gets there
        double flightTicks = center.distanceTo(pivot) / weapon.speed;
        Vec3 aim = center.add(targetVelocity.scale(flightTicks)).subtract(pivot).normalize();

        if (weapon == DefenseTurretBlock.Weapon.CIWS) {
            Vec3 muzzle = pivot.add(aim.scale(1.0D));
            ApacheBullet bullet = new ApacheBullet(SkofcraftEntities.APACHE_BULLET.get(), level);
            bullet.setPos(muzzle.x, muzzle.y, muzzle.z);
            bullet.shoot(aim.x, aim.y, aim.z, weapon.speed, weapon.inaccuracy);
            level.addFreshEntity(bullet);
            level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
            level.playSound(null, muzzle.x, muzzle.y, muzzle.z, SkofcraftSounds.CIWS.get(), SoundSource.BLOCKS, 0.8F, 0.95F + level.random.nextFloat() * 0.1F);
        } else {
            leftTube = !leftTube;
            Vec3 side = new Vec3(aim.z, 0.0D, -aim.x).normalize().scale(leftTube ? 0.2D : -0.2D);
            Vec3 muzzle = pivot.add(aim.scale(0.9D)).add(side);
            ApacheRocket missile = new ApacheRocket(SkofcraftEntities.APACHE_ROCKET.get(), level);
            missile.setPos(muzzle.x, muzzle.y, muzzle.z);
            // A little loft so the missile clears the deck before it turns onto the target
            missile.shoot(aim.x, aim.y + 0.15D, aim.z, weapon.speed, weapon.inaccuracy);
            missile.lockOn(target);
            level.addFreshEntity(missile);
            level.sendParticles(ParticleTypes.CLOUD, muzzle.x, muzzle.y, muzzle.z, 8, 0.2D, 0.2D, 0.2D, 0.02D);
            level.playSound(null, muzzle.x, muzzle.y, muzzle.z, SkofcraftSounds.ROCKET_FIRE.get(), SoundSource.BLOCKS, 2.5F, 1.0F);
        }
    }

    // ---------------------------------------------------------------- client

    public void clientTick() {
        prevYaw = yaw;
        prevPitch = pitch;
        prevSpin = spin;
        Entity tracked = level == null || targetId < 0 ? null : level.getEntity(targetId);
        if (tracked != null) {
            Vec3 delta = tracked.getBoundingBox().getCenter().subtract(pivot());
            float wantYaw = (float) (Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90.0F;
            float wantPitch = (float) (-Mth.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)) * Mth.RAD_TO_DEG);
            yaw += Mth.clamp(Mth.wrapDegrees(wantYaw - yaw), -TURN_SPEED, TURN_SPEED);
            pitch += Mth.clamp(wantPitch - pitch, -TURN_SPEED, TURN_SPEED);
            if (weapon() == DefenseTurretBlock.Weapon.CIWS) {
                spin += 45.0F;
            }
        } else {
            yaw += 1.0F; // idle search sweep
            pitch += (-10.0F - pitch) * 0.1F;
        }
    }

    // ---------------------------------------------------------------- state

    @Nullable
    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public boolean engagesAllThreats() {
        return engageAllThreats;
    }

    public void setEngageAllThreats(boolean engageAllThreats) {
        this.engageAllThreats = engageAllThreats;
        setTarget(null);
        setChanged();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putBoolean("AllThreats", engageAllThreats);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        engageAllThreats = tag.getBoolean("AllThreats");
        if (tag.contains("Target")) {
            targetId = tag.getInt("Target");
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Target", targetId);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
