package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.client.ApacheClientInput;
import ch.rere232.skofcraft.network.FlightDeckC2SPacket;
import ch.rere232.skofcraft.network.JetCrashC2SPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import ch.rere232.skofcraft.registry.SkofcraftBlocks;
import ch.rere232.skofcraft.registry.SkofcraftEntities;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * A two-seat F-14 Tomcat. The pilot sets the throttle with W/S, steers by looking, nudges the rudder with A/D
 * and lights the afterburner with Space. It needs speed to stay up: climbing bleeds speed, and below stall speed
 * the wings lose lift so it falls and the nose drops until it dives back up to speed. It
 * only leaves the ground above take-off speed. The engines burn fuel while someone is at the controls; with an
 * empty tank the throttle dies and the jet glides down. The flying client simulates and the server follows its moves.
 */
public class F14Tomcat extends Entity implements PilotedAircraft {
    public static final float MAX_HEALTH = 60.0F;
    public static final double MAX_SPEED = 1.7D;
    public static final double AFTERBURNER_FACTOR = 1.45D;
    public static final double STALL_SPEED = 0.7D;
    public static final double TAKEOFF_SPEED = 0.95D;
    public static final int MISSILE_COOLDOWN = 40;
    public static final int MAX_FUEL = AircraftFuel.PER_CANISTER * 4;

    private static final int SEATS = 2;
    private static final float TURN_YAW = 4.0F;
    private static final float TURN_PITCH = 3.0F;
    private static final int GUN_COOLDOWN = 2;
    /** Speed lost per tick when flying straight up (scaled by the climb angle). */
    private static final double CLIMB_DRAG = 0.045D;
    /** Downward acceleration with no lift at all, and the fastest the jet can fall. */
    private static final double GRAVITY = 0.08D;
    private static final double MAX_SINK = 2.5D;
    /** Degrees per tick the nose drops with no lift at all. */
    private static final float STALL_NOSE_DROP = 4.5F;
    /** Carrier deck: catapult end speed and push per tick, and the slowest a jet can be going to catch a wire. */
    private static final double CATAPULT_SPEED = 1.5D;
    private static final double CATAPULT_PUSH = 0.12D;
    private static final double HOOK_SPEED = 0.3D;
    private static final double[] SEAT_FORWARD = {1.5D, 0.2D};
    private static final EntityDataAccessor<Float> DATA_HEALTH =
            SynchedEntityData.defineId(F14Tomcat.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_FUEL =
            SynchedEntityData.defineId(F14Tomcat.class, EntityDataSerializers.INT);

    /** Throttle 0..1 and current speed in blocks per tick, as simulated by the flying client. */
    public float throttle;
    public double speed;
    public boolean afterburner;
    public float wingSweep;
    public float wingSweepO;
    public float tiltRoll;
    public float tiltRollO;
    /** Extra fall speed (blocks per tick) building up while the wings have too little lift. */
    private double sinkSpeed;

    private long nextGunTick;
    private long nextMissileTick;
    private boolean missileLeft;
    private boolean crashReported;
    private boolean hooked;
    private boolean catapultFired;

    public F14Tomcat(EntityType<? extends F14Tomcat> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    public F14Tomcat(Level level, double x, double y, double z, float yaw) {
        this(SkofcraftEntities.F14.get(), level);
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

        if (isControlledByLocalInstance()) {
            if (pilot != null) {
                fly(pilot);
            } else {
                settle();
            }
            double verticalBefore = getDeltaMovement().y;
            move(MoverType.SELF, getDeltaMovement());
            if (level().isClientSide && pilot != null) {
                checkCrash(verticalBefore);
            }
            if (horizontalCollision) {
                speed = 0.0D;
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
        animate();

        if (!level().isClientSide && pilot != null) {
            AircraftFuel.burn(this, pilot);
            if (getFuel() > 0 && tickCount % 6 == 0) {
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.NEUTRAL, 2.0F, 1.7F);
            }
        }
    }

    private static Vec3 direction(float yaw, float pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double flat = Math.cos(pitchRad);
        return new Vec3(-Math.sin(yawRad) * flat, -Math.sin(pitchRad), Math.cos(yawRad) * flat);
    }

    private void fly(LivingEntity pilot) {
        boolean fueled = getFuel() > 0;
        if (!fueled) {
            // Engines out: the throttle spools down whatever the pilot does.
            throttle = Math.max(0.0F, throttle - 0.03F);
        } else if (pilot.zza > 0.0F) {
            throttle = Math.min(1.0F, throttle + 0.02F);
        } else if (pilot.zza < 0.0F) {
            throttle = Math.max(0.0F, throttle - 0.03F);
        }
        afterburner = fueled && throttle > 0.9F && Boolean.TRUE.equals(
                DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> ApacheClientInput.isJumping(pilot)));
        double target = throttle * MAX_SPEED * (afterburner ? AFTERBURNER_FACTOR : 1.0D);
        speed += (target - speed) * 0.04D;

        boolean grounded = onGround();
        if (!grounded) {
            hooked = false;
        }
        boolean onCatapult = grounded && flightDeck(pilot, fueled);
        boolean taxiing = grounded && speed < TAKEOFF_SPEED;

        if (!onCatapult) {
            float yawDiff = Mth.wrapDegrees(pilot.getYRot() - getYRot());
            setYRot(getYRot() + Mth.clamp(yawDiff, -TURN_YAW, TURN_YAW) - pilot.xxa * 3.0F);
            if (taxiing) {
                setXRot(getXRot() * 0.8F);
            } else {
                float pitchDiff = pilot.getXRot() - getXRot();
                setXRot(Mth.clamp(getXRot() + Mth.clamp(pitchDiff, -TURN_PITCH, TURN_PITCH), -85.0F, 85.0F));
            }
        }

        if (taxiing) {
            sinkSpeed = 0.0D;
            Vec3 dir = direction(getYRot(), 0.0F);
            setDeltaMovement(dir.x * speed, -0.1D, dir.z * speed);
            return;
        }

        Vec3 dir = direction(getYRot(), getXRot());
        if (!grounded) {
            // Climbing trades speed for height, diving gives it back.
            speed = Math.max(0.0D, speed - dir.y * CLIMB_DRAG);
        }

        // Lift fades out below stall speed: the jet starts to sink and the nose drops until it picks up speed again.
        double lift = Mth.clamp(speed / STALL_SPEED, 0.0D, 1.0D);
        lift *= lift;
        if (grounded) {
            sinkSpeed = 0.0D;
        } else {
            sinkSpeed = Math.min(MAX_SINK, (sinkSpeed + GRAVITY * (1.0D - lift)) * (1.0D - 0.12D * lift));
            if (lift < 1.0D) {
                setXRot(Math.min(85.0F, getXRot() + (float) (1.0D - lift) * STALL_NOSE_DROP));
            }
        }
        setDeltaMovement(dir.x * speed, dir.y * speed - sinkSpeed, dir.z * speed);

        if (getY() >= level().getMaxBuildHeight() - 1 && getDeltaMovement().y > 0.0D) {
            setDeltaMovement(getDeltaMovement().x, 0.0D, getDeltaMovement().z);
        }
    }

    /**
     * Carrier deck gear under the wheels. An arresting wire hooks a jet rolling over it fast and drags it to a stop;
     * a catapult holds the jet lined up until full throttle, then flings it toward the bow. Returns true while the
     * catapult has the jet.
     */
    private boolean flightDeck(LivingEntity pilot, boolean fueled) {
        BlockState below = level().getBlockState(BlockPos.containing(getX(), getY() - 0.2D, getZ()));
        if (below.is(SkofcraftBlocks.ARRESTING_WIRE.get()) && speed > HOOK_SPEED && !hooked) {
            hooked = true;
            deckEvent(pilot, FlightDeckC2SPacket.TRAP, SoundEvents.CHAIN_BREAK, 0.6F);
        }
        if (hooked) {
            throttle = 0.0F;
            speed *= 0.82D;
            if (speed < 0.05D) {
                speed = 0.0D;
                hooked = false;
            }
        }
        if (!below.is(SkofcraftBlocks.CATAPULT.get())) {
            catapultFired = false;
            return false;
        }
        setYRot(below.getValue(HorizontalDirectionalBlock.FACING).toYRot());
        setXRot(0.0F);
        if (!fueled || throttle < 0.9F) {
            speed = 0.0D; // holdback bar until the engines are at full power
            return true;
        }
        if (!catapultFired) {
            catapultFired = true;
            deckEvent(pilot, FlightDeckC2SPacket.CATAPULT, SoundEvents.PISTON_EXTEND, 0.5F);
        }
        speed = Math.min(CATAPULT_SPEED, speed + CATAPULT_PUSH);
        return true;
    }

    private void deckEvent(LivingEntity pilot, int event, SoundEvent sound, float pitch) {
        if (!level().isClientSide) {
            return;
        }
        level().playSound(pilot instanceof Player player ? player : null, getX(), getY(), getZ(), sound, SoundSource.NEUTRAL, 2.0F, pitch);
        SkofcraftNetwork.CHANNEL.sendToServer(new FlightDeckC2SPacket(event));
    }

    /** Without a pilot the jet rolls to a stop and settles on the ground. */
    private void settle() {
        Vec3 v = getDeltaMovement();
        speed *= 0.9D;
        throttle = 0.0F;
        setDeltaMovement(v.x * 0.9D, Math.max(v.y - 0.08D, -0.8D) * 0.98D, v.z * 0.9D);
    }

    /** A hard hit at speed destroys the jet; the server only trusts the pilot's own report of it. */
    private void checkCrash(double verticalBefore) {
        boolean wall = horizontalCollision && speed > 0.7D;
        boolean ground = verticalCollision && (verticalBefore < -0.8D || (getXRot() > 30.0F && speed > 0.9D));
        if ((wall || ground) && !crashReported) {
            crashReported = true;
            SkofcraftNetwork.CHANNEL.sendToServer(new JetCrashC2SPacket());
        }
    }

    /** Server only: the pilot reported an impact. */
    public void crash() {
        if (isRemoved()) {
            return;
        }
        AircraftWreck.awardCrew(this);
        ejectPassengers();
        discard();
        level().explode(null, getX(), getY(), getZ(), 3.0F, Level.ExplosionInteraction.NONE);
        AircraftWreck.scatterF14(this);
    }

    private void animate() {
        double dx = getX() - xo;
        double dy = getY() - yo;
        double dz = getZ() - zo;
        double observed = Math.sqrt(dx * dx + dy * dy + dz * dz);

        wingSweepO = wingSweep;
        float sweepTarget = Mth.clamp((float) ((observed - 0.5D) / 1.0D), 0.0F, 1.0F);
        wingSweep += (sweepTarget - wingSweep) * 0.1F;

        tiltRollO = tiltRoll;
        float yawRate = Mth.wrapDegrees(getYRot() - yRotO);
        tiltRoll += (Mth.clamp(-yawRate * 9.0F, -60.0F, 60.0F) - tiltRoll) * 0.2F;
    }

    @Override
    public void fire(ServerPlayer pilot, int weapon) {
        if (getControllingPassenger() != pilot) {
            return;
        }
        long now = level().getGameTime();
        Vec3 forward = direction(getYRot(), getXRot());
        double yawRad = Math.toRadians(getYRot());
        Vec3 left = new Vec3(Math.cos(yawRad), 0.0D, Math.sin(yawRad));

        if (weapon == WEAPON_GUN && now >= nextGunTick) {
            nextGunTick = now + GUN_COOLDOWN;
            Vec3 muzzle = position().add(forward.scale(4.6D)).add(0.0D, 1.5D, 0.0D);
            AircraftWeapons.launch(level(), new ApacheBullet(level(), pilot), muzzle, pilot, 4.0F, 0.8F);
            level().playSound(null, muzzle.x, muzzle.y, muzzle.z, SoundEvents.SKELETON_SHOOT, SoundSource.NEUTRAL, 0.7F, 1.6F);
        } else if (weapon == WEAPON_MISSILE && now >= nextMissileTick) {
            nextMissileTick = now + MISSILE_COOLDOWN;
            missileLeft = !missileLeft;
            ApacheRocket missile = new ApacheRocket(level(), pilot);
            missile.setHoming(true);
            Vec3 muzzle = position().add(forward.scale(1.2D)).add(left.scale(missileLeft ? 2.4D : -2.4D)).add(0.0D, 1.0D, 0.0D);
            AircraftWeapons.launch(level(), missile, muzzle, pilot, 3.0F, 0.0F);
            level().playSound(null, muzzle.x, muzzle.y, muzzle.z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.NEUTRAL, 2.0F, 0.7F);
        }
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (!level().isClientSide && getControllingPassenger() == passenger) {
            long armed = level().getGameTime() + BOARDING_LOCKOUT_TICKS;
            nextGunTick = Math.max(nextGunTick, armed);
            nextMissileTick = Math.max(nextMissileTick, armed);
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
            player.displayClientMessage(Component.translatable("message.skofcraft.f14_controls"), true);
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
                spawnAtLocation(AircraftFuel.withFuel(new ItemStack(SkofcraftItems.F14.get()), getFuel()));
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
            level().explode(null, getX(), getY(), getZ(), 3.0F, Level.ExplosionInteraction.NONE);
            AircraftWreck.scatterF14(this);
        }
        return true;
    }

    @Override
    public float getHealth() {
        return entityData.get(DATA_HEALTH);
    }

    @Override
    public float getMaxHealth() {
        return MAX_HEALTH;
    }

    @Override
    public int secondaryCooldownTicks() {
        return MISSILE_COOLDOWN;
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
        callback.accept(passenger, getX() - Math.sin(yawRad) * forward, getY() + 1.9D + passenger.getMyRidingOffset(),
                getZ() + Math.cos(yawRad) * forward);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(SkofcraftItems.F14.get());
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(7.0D);
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
