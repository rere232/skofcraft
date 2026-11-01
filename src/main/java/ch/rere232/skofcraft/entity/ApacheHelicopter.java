package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.client.ApacheClientInput;
import ch.rere232.skofcraft.registry.SkofcraftEntities;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * A rideable attack helicopter. The first rider flies: look to turn, W/S forward and back, A/D strafe,
 * Space climbs, C descends, attack fires the chain gun and use fires rockets. The engine burns fuel while
 * someone is at the controls; with an empty tank the rotor spins down and it sinks. The flying client
 * simulates the movement and the server accepts it through the vehicle move packet.
 */
public class ApacheHelicopter extends Entity implements PilotedAircraft {
    public static final int WEAPON_GUN = 0;
    public static final int WEAPON_ROCKET = 1;

    private static final int SEATS = 2;
    private static final double SPEED = 0.7D;
    private static final double CLIMB_SPEED = 0.35D;
    private static final float TURN_RATE = 6.0F;
    private static final int GUN_COOLDOWN = 2;
    public static final int ROCKET_COOLDOWN = 15;
    public static final float MAX_HEALTH = 40.0F;
    public static final int MAX_FUEL = AircraftFuel.PER_CANISTER * 3;
    private static final double[] SEAT_FORWARD = {0.25D, 1.4D};
    private static final EntityDataAccessor<Float> DATA_HEALTH =
            SynchedEntityData.defineId(ApacheHelicopter.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_FUEL =
            SynchedEntityData.defineId(ApacheHelicopter.class, EntityDataSerializers.INT);

    private long nextGunTick;
    private long nextRocketTick;
    private boolean rocketLeft;

    public float rotorAngle;
    public float rotorAngleO;
    public float tiltPitch;
    public float tiltPitchO;
    public float tiltRoll;
    public float tiltRollO;
    private float rotorSpeed;

    public ApacheHelicopter(EntityType<? extends ApacheHelicopter> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    public ApacheHelicopter(Level level, double x, double y, double z, float yaw) {
        this(SkofcraftEntities.APACHE.get(), level);
        setPos(x, y, z);
        setYRot(yaw);
        xo = x;
        yo = y;
        zo = z;
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity pilot = getControllingPassenger();
        boolean piloted = pilot != null;
        boolean running = piloted && getFuel() > 0;

        if (isControlledByLocalInstance()) {
            if (running) {
                fly(pilot);
            } else {
                settle();
            }
            move(MoverType.SELF, getDeltaMovement());
            if (horizontalCollision) {
                setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
            }
            if (verticalCollision) {
                setDeltaMovement(getDeltaMovement().x, 0.0D, getDeltaMovement().z);
            }
        }

        for (Entity passenger : getPassengers()) {
            passenger.resetFallDistance();
        }
        resetFallDistance();
        animate(running);

        if (!level().isClientSide && piloted) {
            AircraftFuel.burn(this, pilot);
            if (running && tickCount % 4 == 0) {
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.NEUTRAL, 1.4F, 0.45F);
            }
        }
    }

    private void fly(LivingEntity pilot) {
        float diff = Mth.wrapDegrees(pilot.getYRot() - getYRot());
        setYRot(getYRot() + Mth.clamp(diff, -TURN_RATE, TURN_RATE));
        setXRot(0.0F);

        double yawRad = Math.toRadians(getYRot());
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        Vec3 left = new Vec3(Math.cos(yawRad), 0.0D, Math.sin(yawRad));
        Vec3 wanted = forward.scale(pilot.zza).add(left.scale(pilot.xxa));
        if (wanted.lengthSqr() > 1.0D) {
            wanted = wanted.normalize();
        }
        wanted = wanted.scale(SPEED);
        boolean up = Boolean.TRUE.equals(DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> ApacheClientInput.isJumping(pilot)));
        boolean down = Boolean.TRUE.equals(DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> ApacheClientInput.isDescending(pilot)));
        double vertical = up ? CLIMB_SPEED : (down ? -CLIMB_SPEED : 0.0D);

        Vec3 v = getDeltaMovement();
        setDeltaMovement(
                v.x + (wanted.x - v.x) * 0.12D,
                v.y + (vertical - v.y) * 0.2D,
                v.z + (wanted.z - v.z) * 0.12D);
        if (getY() >= level().getMaxBuildHeight() - 1 && getDeltaMovement().y > 0.0D) {
            setDeltaMovement(getDeltaMovement().x, 0.0D, getDeltaMovement().z);
        }
    }

    /** Without a pilot or fuel the helicopter coasts to a stop and sinks to the ground. */
    private void settle() {
        Vec3 v = getDeltaMovement();
        setDeltaMovement(v.x * 0.9D, Math.max(v.y - 0.04D, -0.5D) * 0.98D, v.z * 0.9D);
    }

    private void animate(boolean running) {
        rotorAngleO = rotorAngle;
        rotorSpeed += ((running ? 60.0F : 0.0F) - rotorSpeed) * (running ? 0.08F : 0.03F);
        rotorAngle += rotorSpeed;

        double yawRad = Math.toRadians(getYRot());
        double dx = getX() - xo;
        double dz = getZ() - zo;
        double forwardSpeed = -Math.sin(yawRad) * dx + Math.cos(yawRad) * dz;
        double leftSpeed = Math.cos(yawRad) * dx + Math.sin(yawRad) * dz;
        tiltPitchO = tiltPitch;
        tiltRollO = tiltRoll;
        tiltPitch += (Mth.clamp((float) forwardSpeed * 22.0F, -12.0F, 12.0F) - tiltPitch) * 0.2F;
        tiltRoll += (Mth.clamp((float) leftSpeed * 28.0F, -14.0F, 14.0F) - tiltRoll) * 0.2F;
    }

    /** Called on the server for the pilot's fire packets. */
    @Override
    public void fire(ServerPlayer pilot, int weapon) {
        if (getControllingPassenger() != pilot) {
            return;
        }
        long now = level().getGameTime();
        if (weapon == WEAPON_GUN && now >= nextGunTick) {
            nextGunTick = now + GUN_COOLDOWN;
            ApacheBullet bullet = new ApacheBullet(level(), pilot);
            Vec3 muzzle = muzzle(0.0D, 1.4D, 3.2D);
            launch(bullet, muzzle, pilot, 3.2F, 0.6F);
            level().playSound(null, muzzle.x, muzzle.y, muzzle.z, SoundEvents.SKELETON_SHOOT, SoundSource.NEUTRAL, 0.6F, 1.9F);
        } else if (weapon == WEAPON_ROCKET && now >= nextRocketTick) {
            nextRocketTick = now + ROCKET_COOLDOWN;
            rocketLeft = !rocketLeft;
            ApacheRocket rocket = new ApacheRocket(level(), pilot);
            Vec3 muzzle = muzzle(rocketLeft ? 1.8D : -1.8D, 1.25D, 0.8D);
            launch(rocket, muzzle, pilot, 1.8F, 0.0F);
            level().playSound(null, muzzle.x, muzzle.y, muzzle.z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.NEUTRAL, 1.5F, 0.8F);
        }
    }

    /** A point relative to the helicopter: left, up, forward. */
    private Vec3 muzzle(double left, double up, double forward) {
        double yawRad = Math.toRadians(getYRot());
        double fx = -Math.sin(yawRad);
        double fz = Math.cos(yawRad);
        return new Vec3(getX() + fx * forward + fz * left, getY() + up, getZ() + fz * forward - fx * left);
    }

    private void launch(net.minecraft.world.entity.projectile.Projectile projectile, Vec3 muzzle, LivingEntity pilot,
                        float velocity, float inaccuracy) {
        Vec3 aim = pilot.getEyePosition().add(pilot.getLookAngle().scale(60.0D)).subtract(muzzle).normalize();
        projectile.setPos(muzzle.x, muzzle.y, muzzle.z);
        projectile.shoot(aim.x, aim.y, aim.z, velocity, inaccuracy);
        level().addFreshEntity(projectile);
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (!level().isClientSide && getControllingPassenger() == passenger) {
            long armed = level().getGameTime() + BOARDING_LOCKOUT_TICKS;
            nextGunTick = Math.max(nextGunTick, armed);
            nextRocketTick = Math.max(nextRocketTick, armed);
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (AircraftFuel.tryRefuel(this, this, player, hand)) {
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (player.isSecondaryUseActive() || getPassengers().size() >= SEATS) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide && player.startRiding(this)) {
            player.displayClientMessage(Component.translatable("message.skofcraft.apache_controls"), true);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved() || isInvulnerableTo(source)
                || (source.getEntity() != null && hasPassenger(source.getEntity()))) {
            return false;
        }
        if (source.getDirectEntity() instanceof Player player && !isVehicle()) {
            if (!player.getAbilities().instabuild) {
                spawnAtLocation(AircraftFuel.withFuel(new ItemStack(SkofcraftItems.APACHE.get()), getFuel()));
            }
            discard();
            return true;
        }
        float remaining = getHealth() - amount;
        entityData.set(DATA_HEALTH, remaining);
        if (remaining <= 0.0F) {
            AircraftWreck.awardCrew(this);
            ejectPassengers();
            // Removed first: the blast would otherwise hurt this entity again and recurse.
            discard();
            level().explode(null, getX(), getY(), getZ(), 2.5F, Level.ExplosionInteraction.NONE);
            AircraftWreck.scatterApache(this);
        }
        return true;
    }

    public float getHealth() {
        return entityData.get(DATA_HEALTH);
    }

    @Override
    public float getMaxHealth() {
        return MAX_HEALTH;
    }

    @Override
    public int secondaryCooldownTicks() {
        return ROCKET_COOLDOWN;
    }

    @Override
    public int getFuel() {
        return entityData.get(DATA_FUEL);
    }

    @Override
    public int getMaxFuel() {
        return MAX_FUEL;
    }

    @Override
    public void setFuel(int fuel) {
        entityData.set(DATA_FUEL, Mth.clamp(fuel, 0, MAX_FUEL));
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < SEATS;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        if (!hasPassenger(passenger)) {
            return;
        }
        int seat = Math.min(getPassengers().indexOf(passenger), SEAT_FORWARD.length - 1);
        double yawRad = Math.toRadians(getYRot());
        double forward = SEAT_FORWARD[seat];
        callback.accept(passenger, getX() - Math.sin(yawRad) * forward, getY() + 1.4D + passenger.getMyRidingOffset(),
                getZ() + Math.cos(yawRad) * forward);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(SkofcraftItems.APACHE.get());
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(5.0D);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_HEALTH, MAX_HEALTH);
        entityData.define(DATA_FUEL, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DATA_HEALTH, tag.contains("Health") ? tag.getFloat("Health") : MAX_HEALTH);
        setFuel(tag.getInt(AircraftFuel.TAG));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Health", getHealth());
        tag.putInt(AircraftFuel.TAG, getFuel());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
