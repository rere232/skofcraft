package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.registry.SkofcraftEntities;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import ch.rere232.skofcraft.registry.SkofcraftSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * A Swiss postal bus. The first rider drives: W/S throttle and brake, A/D steer, and the horn key plays the real
 * three-tone horn (C sharp, E, A). Like the aircraft, the driving client simulates the movement and the server
 * accepts it through the vehicle move packet. It runs on jet fuel, because the timetable is not negotiable.
 */
public class Postbus extends Entity implements FueledVehicle {
    public static final float MAX_HEALTH = 60.0F;
    public static final int MAX_FUEL = AircraftFuel.PER_CANISTER * 3;

    private static final double MAX_SPEED = 0.6D;
    private static final double MAX_REVERSE = 0.15D;
    private static final double ACCELERATION = 0.012D;
    private static final double BRAKING = 0.03D;
    private static final float TURN_RATE = 3.5F;
    /** Length of the horn clip, so it can't be stacked on top of itself. */
    private static final int HORN_COOLDOWN = 110;
    private static final double WHEEL_RADIUS = 0.375D;
    private static final float STEER_PER_DEGREE = 0.13F;
    private static final float MAX_STEER = 0.45F;
    private static final double DOOR_DISTANCE = 2.2D;
    /**
     * Seats as forward, left, which the cabin model builds its seats from. The driver comes first; the bus is
     * left-hand drive, and the gap between the second and third rows is for the middle door.
     */
    public static final double[][] SEATS = {
            {3.4D, 0.6D},
            {2.4D, 0.6D}, {2.4D, -0.6D},
            {1.2D, 0.6D}, {1.2D, -0.6D},
            {-1.0D, 0.6D}, {-1.0D, -0.6D},
            {-2.5D, 0.6D}, {-2.5D, -0.6D}
    };

    private static final EntityDataAccessor<Float> DATA_HEALTH =
            SynchedEntityData.defineId(Postbus.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_FUEL =
            SynchedEntityData.defineId(Postbus.class, EntityDataSerializers.INT);

    private double speed;
    private float deltaYaw;
    private int hornCooldown;

    public float wheelAngle;
    public float wheelAngleO;
    public float steer;
    public float steerO;
    /** Blocks per tick, for the speedometer. */
    public float shownSpeed;
    public float shownSpeedO;
    private double lastX;
    private double lastZ;
    private float lastYaw;

    public Postbus(EntityType<? extends Postbus> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        setMaxUpStep(1.1F);
    }

    public Postbus(Level level, double x, double y, double z, float yaw) {
        this(SkofcraftEntities.POSTBUS.get(), level);
        setPos(x, y, z);
        setYRot(yaw);
        xo = x;
        yo = y;
        zo = z;
        lastX = x;
        lastZ = z;
        lastYaw = yaw;
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity driver = getControllingPassenger();
        deltaYaw = 0.0F;

        if (isControlledByLocalInstance()) {
            if (driver != null && getFuel() > 0) {
                drive(driver);
            } else {
                speed *= 0.92D;
            }
            applyMovement();
        }
        for (Entity passenger : getPassengers()) {
            passenger.resetFallDistance();
        }
        animate();

        if (!level().isClientSide) {
            if (hornCooldown > 0) {
                hornCooldown--;
            }
            if (driver != null) {
                AircraftFuel.burn(this, driver);
            }
        }
    }

    private void drive(LivingEntity driver) {
        if (driver.zza > 0.0F) {
            speed = Math.min(MAX_SPEED, speed + (speed < 0.0D ? BRAKING : ACCELERATION));
        } else if (driver.zza < 0.0F) {
            speed = Math.max(-MAX_REVERSE, speed - (speed > 0.0D ? BRAKING : ACCELERATION));
        } else {
            speed *= 0.985D;
        }
        // Buses don't turn on the spot, and steering swaps round in reverse
        float grip = Mth.clamp((float) Math.abs(speed) / 0.2F, 0.0F, 1.0F);
        deltaYaw = -driver.xxa * TURN_RATE * grip * (speed < 0.0D ? -1.0F : 1.0F);
        setYRot(getYRot() + deltaYaw);
    }

    private void applyMovement() {
        double yawRad = Math.toRadians(getYRot());
        double vertical = onGround() ? -0.04D : Math.max(getDeltaMovement().y - 0.08D, -2.0D) * 0.98D;
        setDeltaMovement(-Math.sin(yawRad) * speed, vertical, Math.cos(yawRad) * speed);
        move(MoverType.SELF, getDeltaMovement());
        if (horizontalCollision) {
            speed = 0.0D;
        }
    }

    /** Works from the position itself, so the wheels also turn for everyone who isn't driving. */
    private void animate() {
        double yawRad = Math.toRadians(getYRot());
        double forwardSpeed = -Math.sin(yawRad) * (getX() - lastX) + Math.cos(yawRad) * (getZ() - lastZ);
        wheelAngleO = wheelAngle;
        wheelAngle += (float) (forwardSpeed / WHEEL_RADIUS);
        shownSpeedO = shownSpeed;
        shownSpeed += (Math.abs((float) forwardSpeed) - shownSpeed) * 0.25F;

        steerO = steer;
        float wanted = Mth.clamp(Mth.wrapDegrees(getYRot() - lastYaw) * STEER_PER_DEGREE, -MAX_STEER, MAX_STEER);
        steer += (wanted - steer) * 0.3F;

        lastX = getX();
        lastZ = getZ();
        lastYaw = getYRot();
    }

    /** Called on the server for the driver's horn packets. */
    public void honk(ServerPlayer driver) {
        if (getControllingPassenger() != driver || hornCooldown > 0) {
            return;
        }
        hornCooldown = HORN_COOLDOWN;
        // Bound to the entity, so the horn travels with the bus
        level().playSound(null, this, SkofcraftSounds.POSTBUS_HORN.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        SkofcraftAdvancements.award(driver, SkofcraftAdvancements.DU_DA_DO);
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (getControllingPassenger() == passenger) {
            speed = 0.0D;
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (AircraftFuel.tryRefuel(this, this, player, hand)) {
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (player.isSecondaryUseActive() || !canAddPassenger(player)) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide && player.startRiding(this)) {
            player.displayClientMessage(Component.translatable("message.skofcraft.postbus_controls"), true);
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
                spawnAtLocation(AircraftFuel.withFuel(new ItemStack(SkofcraftItems.POSTBUS.get()), getFuel()));
            }
            discard();
            return true;
        }
        float remaining = getHealth() - amount;
        entityData.set(DATA_HEALTH, remaining);
        if (remaining <= 0.0F) {
            ejectPassengers();
            // Removed first: the blast would otherwise hurt this entity again and recurse.
            discard();
            level().explode(null, getX(), getY(), getZ(), 2.0F, Level.ExplosionInteraction.NONE);
            spawnAtLocation(new ItemStack(SkofcraftItems.GEAR.get(), 2));
            spawnAtLocation(new ItemStack(SkofcraftItems.AIRCRAFT_PLATE.get(), 2));
        }
        return true;
    }

    public float fuelFraction() {
        return (float) getFuel() / MAX_FUEL;
    }

    public float getHealth() {
        return entityData.get(DATA_HEALTH);
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
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < SEATS.length;
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
        double[] seat = SEATS[Math.min(getPassengers().indexOf(passenger), SEATS.length - 1)];
        Vec3 pos = relative(seat[0], seat[1]);
        callback.accept(passenger, pos.x, getY() + 0.9D + passenger.getMyRidingOffset(), pos.z);
        // Everyone on board turns with the bus
        passenger.setYRot(passenger.getYRot() + deltaYaw);
        passenger.setYHeadRot(passenger.getYHeadRot() + deltaYaw);
    }

    /** Out through the doors on the right-hand side, or over the roof if something is parked there. */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 door = relative(0.0D, -DOOR_DISTANCE);
        AABB box = passenger.getDimensions(Pose.STANDING).makeBoundingBox(door);
        return level().noCollision(passenger, box) ? door : super.getDismountLocationForPassenger(passenger);
    }

    /** A point at this bus's height: forward, left. */
    private Vec3 relative(double forward, double left) {
        double yawRad = Math.toRadians(getYRot());
        double fx = -Math.sin(yawRad);
        double fz = Math.cos(yawRad);
        return new Vec3(getX() + fx * forward + fz * left, getY(), getZ() + fz * forward - fx * left);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(SkofcraftItems.POSTBUS.get());
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(6.0D);
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
