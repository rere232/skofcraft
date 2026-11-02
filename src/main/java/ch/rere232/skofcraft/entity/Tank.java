package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.registry.SkofcraftItems;
import ch.rere232.skofcraft.registry.SkofcraftSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * A driveable tank. W/S drive, A/D pivot the hull, and the turret follows where the driver looks. Attack fires the
 * machine gun, use fires the main gun, and both go where the barrel is pointing, not where you're looking. It rolls
 * over small blocks and whoever is standing in the way. Like the aircraft, the driving client simulates the movement
 * and the server accepts it through the vehicle move packet.
 * <p>
 * Each tank is a subclass that only supplies its {@link Spec}.
 */
public abstract class Tank extends Entity implements PilotedAircraft {
    /** How it moves. Speeds are in blocks per tick, turn rates in degrees per tick. */
    public record Mobility(double maxSpeed, double maxReverse, double acceleration, double braking, float hullTurnRate) {}

    /** The main gun. The pivot is relative to the tank and the muzzle distance is measured from the pivot. */
    public record Gun(int reloadTicks, float shellPower, float soundPitch, float turretTurnRate,
                      double pivotForward, double pivotHeight, double muzzleDistance) {}

    /**
     * Where everything is. The driver's seat is forward, height, head out of the hatch; deck seats are forward, left,
     * height, for anyone else. A seat's height is the surface it sits on minus 0.6, which is where a sitting body's hips
     * are.
     */
    public record Layout(double[] driverSeat, double[][] deckSeats, double crushForward, double crushReach, double cullMargin) {}

    public record Spec(Supplier<? extends Item> item, float maxHealth, float armor, int maxFuel, float crushDamage, float enginePitch,
                       Mobility mobility, Gun gun, Layout layout) {}

    private static final float GUN_MAX_UP = -20.0F;
    private static final float GUN_MAX_DOWN = 10.0F;
    private static final int GUN_COOLDOWN = 3;
    private static final float CRUSHABLE_HARDNESS = 0.6F;
    private static final double MIN_CRUSH_SPEED = 0.05D;
    private static final int ENGINE_INTERVAL = 10;

    private static final EntityDataAccessor<Float> DATA_HEALTH = SynchedEntityData.defineId(Tank.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_FUEL = SynchedEntityData.defineId(Tank.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_TURRET = SynchedEntityData.defineId(Tank.class, EntityDataSerializers.FLOAT);

    private double speed;
    private long nextGunTick;
    private long nextCannonTick;

    /** World yaw, like the hull's: the turret keeps its bearing while the hull turns under it. */
    public float turretYaw;
    public float turretYawO;
    public float gunPitch;
    public float gunPitchO;
    public float shownSpeed;
    private double lastX;
    private double lastZ;

    protected Tank(EntityType<? extends Tank> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        setMaxUpStep(1.1F);
    }

    /**
     * Has to come from a constant in the subclass, not a field set in a constructor: {@link Entity}'s constructor calls
     * {@link #defineSynchedData()}, which needs it, before any field here has been set.
     */
    public abstract Spec spec();

    /** A player has just taken the driver's seat. */
    protected abstract void onDriverBoarded(Entity driver);

    /** Puts a newly created tank in the world's coordinates, with the turret facing the way the hull does. */
    public void place(double x, double y, double z, float yaw) {
        setPos(x, y, z);
        setYRot(yaw);
        xo = x;
        yo = y;
        zo = z;
        lastX = x;
        lastZ = z;
        turretYaw = yaw;
        turretYawO = yaw;
        entityData.set(DATA_TURRET, yaw);
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity driver = getControllingPassenger();
        boolean running = driver != null && getFuel() > 0;

        if (isControlledByLocalInstance()) {
            if (running) {
                drive(driver);
            } else {
                speed *= 0.9D;
            }
            double yawRad = Math.toRadians(getYRot());
            double vertical = onGround() ? -0.04D : Math.max(getDeltaMovement().y - 0.08D, -2.0D) * 0.98D;
            setDeltaMovement(-Math.sin(yawRad) * speed, vertical, Math.cos(yawRad) * speed);
            move(MoverType.SELF, getDeltaMovement());
            if (horizontalCollision) {
                speed = 0.0D;
            }
        }
        aimTurret(driver, running);
        for (Entity passenger : getPassengers()) {
            passenger.resetFallDistance();
        }
        resetFallDistance();

        double dx = getX() - lastX;
        double dz = getZ() - lastZ;
        lastX = getX();
        lastZ = getZ();
        double moved = Math.sqrt(dx * dx + dz * dz);
        shownSpeed += ((float) moved - shownSpeed) * 0.25F;

        if (level() instanceof ServerLevel serverLevel && driver != null) {
            AircraftFuel.burn(this, driver);
            if (running && tickCount % ENGINE_INTERVAL == 0) {
                serverLevel.playSound(null, getX(), getY(), getZ(), SkofcraftSounds.TANK_ENGINE.get(), SoundSource.NEUTRAL, 2.0F, spec().enginePitch());
            }
            if (moved > MIN_CRUSH_SPEED && tickCount % 2 == 0) {
                crush(serverLevel, driver, dx, dz, moved);
            }
        }
    }

    private void drive(LivingEntity driver) {
        Mobility mobility = spec().mobility();
        if (driver.zza > 0.0F) {
            speed = Math.min(mobility.maxSpeed(), speed + (speed < 0.0D ? mobility.braking() : mobility.acceleration()));
        } else if (driver.zza < 0.0F) {
            speed = Math.max(-mobility.maxReverse(), speed - (speed > 0.0D ? mobility.braking() : mobility.acceleration()));
        } else {
            speed *= 0.97D;
        }
        // Tracks can pivot on the spot, which a bus can't
        setYRot(getYRot() - driver.xxa * mobility.hullTurnRate());
    }

    /** Both sides work this out from the driver's view, and without a driver clients take the server's word for it. */
    private void aimTurret(@Nullable LivingEntity driver, boolean running) {
        turretYawO = turretYaw;
        gunPitchO = gunPitch;
        if (running) {
            float rate = spec().gun().turretTurnRate();
            turretYaw += Mth.clamp(Mth.wrapDegrees(driver.getYRot() - turretYaw), -rate, rate);
            float wantedPitch = Mth.clamp(driver.getXRot(), GUN_MAX_UP, GUN_MAX_DOWN);
            gunPitch += Mth.clamp(wantedPitch - gunPitch, -rate, rate);
        } else if (level().isClientSide) {
            turretYaw = entityData.get(DATA_TURRET);
        }
        if (!level().isClientSide) {
            entityData.set(DATA_TURRET, turretYaw);
        }
    }

    /** Flattens whatever is in front: anything soft, and fences and doors, if griefing is allowed. Mobs either way. */
    private void crush(ServerLevel level, LivingEntity driver, double dx, double dz, double moved) {
        Layout layout = spec().layout();
        Vec3 heading = new Vec3(dx / moved, 0.0D, dz / moved);
        Vec3 front = position().add(heading.scale(layout.crushForward()));
        AABB zone = new AABB(front, front).inflate(layout.crushReach(), 0.0D, layout.crushReach()).expandTowards(0.0D, 3.0D, 0.0D);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, zone, entity -> !hasPassenger(entity) && !entity.isSpectator() && !(entity instanceof AnimeCrew))) {
            victim.hurt(driver instanceof Player player ? level.damageSources().playerAttack(player) : level.damageSources().generic(), spec().crushDamage());
        }
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(zone.minX, getY() + 1.0D, zone.minZ), BlockPos.containing(zone.maxX, getY() + 3.0D, zone.maxZ))) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && isCrushable(state, level, pos)) {
                level.destroyBlock(pos, true);
            }
        }
    }

    private static boolean isCrushable(BlockState state, Level level, BlockPos pos) {
        if (state.is(BlockTags.FENCES) || state.is(BlockTags.FENCE_GATES) || state.is(BlockTags.WOODEN_DOORS) || state.is(BlockTags.LEAVES)) {
            return true;
        }
        float hardness = state.getDestroySpeed(level, pos);
        return hardness >= 0.0F && hardness <= CRUSHABLE_HARDNESS && !state.is(BlockTags.DIRT) && !state.is(BlockTags.SAND)
                && state.getFluidState().isEmpty();
    }

    private Vec3 gunDirection() {
        double yaw = Math.toRadians(turretYaw);
        double pitch = Math.toRadians(gunPitch);
        return new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
    }

    private Vec3 muzzle(Vec3 direction) {
        Gun gun = spec().gun();
        double hullYaw = Math.toRadians(getYRot());
        Vec3 pivot = new Vec3(getX() - Math.sin(hullYaw) * gun.pivotForward(), getY() + gun.pivotHeight(), getZ() + Math.cos(hullYaw) * gun.pivotForward());
        return pivot.add(direction.scale(gun.muzzleDistance()));
    }

    /** Called on the server for the driver's fire packets. */
    @Override
    public void fire(ServerPlayer driver, int weapon) {
        if (getControllingPassenger() != driver) {
            return;
        }
        Gun gun = spec().gun();
        long now = level().getGameTime();
        Vec3 direction = gunDirection();
        Vec3 muzzle = muzzle(direction);
        if (weapon == WEAPON_GUN && now >= nextGunTick) {
            nextGunTick = now + GUN_COOLDOWN;
            ApacheBullet bullet = new ApacheBullet(level(), driver);
            bullet.setPos(muzzle.x, muzzle.y, muzzle.z);
            bullet.shoot(direction.x, direction.y, direction.z, 3.0F, 1.0F);
            level().addFreshEntity(bullet);
            level().playSound(null, muzzle.x, muzzle.y, muzzle.z, SkofcraftSounds.CHAIN_GUN.get(), SoundSource.NEUTRAL, 0.6F, 1.1F);
        } else if (weapon == WEAPON_MISSILE && now >= nextCannonTick) {
            nextCannonTick = now + gun.reloadTicks();
            TankShell shell = new TankShell(level(), driver, gun.shellPower());
            shell.setPos(muzzle.x, muzzle.y, muzzle.z);
            shell.shoot(direction.x, direction.y, direction.z, TankShell.SPEED, 0.0F);
            level().addFreshEntity(shell);
            level().playSound(null, muzzle.x, muzzle.y, muzzle.z, SkofcraftSounds.TANK_CANNON.get(), SoundSource.NEUTRAL, 6.0F, gun.soundPitch());
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, muzzle.x, muzzle.y, muzzle.z, 2, 0.2D, 0.2D, 0.2D, 0.0D);
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 20, 0.4D, 0.3D, 0.4D, 0.02D);
            }
        }
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (!level().isClientSide && getControllingPassenger() == passenger) {
            speed = 0.0D;
            long armed = level().getGameTime() + BOARDING_LOCKOUT_TICKS;
            nextGunTick = Math.max(nextGunTick, armed);
            nextCannonTick = Math.max(nextCannonTick, armed);
            onDriverBoarded(passenger);
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
            player.displayClientMessage(Component.translatable("message.skofcraft.maus_controls"), true);
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
                spawnAtLocation(AircraftFuel.withFuel(new ItemStack(spec().item().get()), getFuel()));
            }
            discard();
            return true;
        }
        if (amount < spec().armor()) {
            level().playSound(null, getX(), getY() + 1.5D, getZ(), SkofcraftSounds.HULL_CLANG.get(), SoundSource.NEUTRAL, 0.4F, 1.6F);
            return false;
        }
        float remaining = getHealth() - amount;
        entityData.set(DATA_HEALTH, remaining);
        if (remaining <= 0.0F) {
            AircraftWreck.awardCrew(this);
            ejectPassengers();
            // Removed first: the blast would otherwise hurt this entity again and recurse.
            discard();
            level().explode(null, getX(), getY() + 1.0D, getZ(), 4.0F, Level.ExplosionInteraction.NONE);
            spawnAtLocation(new ItemStack(SkofcraftItems.AIRCRAFT_PLATE.get(), 6));
            spawnAtLocation(new ItemStack(SkofcraftItems.GEAR.get(), 4));
        }
        return true;
    }

    @Override
    public float getHealth() {
        return entityData.get(DATA_HEALTH);
    }

    @Override
    public float getMaxHealth() {
        return spec().maxHealth();
    }

    @Override
    public int secondaryCooldownTicks() {
        return spec().gun().reloadTicks();
    }

    @Override
    public int getFuel() {
        return entityData.get(DATA_FUEL);
    }

    @Override
    public int getMaxFuel() {
        return spec().maxFuel();
    }

    @Override
    public void setFuel(int fuel) {
        entityData.set(DATA_FUEL, Mth.clamp(fuel, 0, getMaxFuel()));
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
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && getControllingPassenger() == null || hasCrewSeat();
    }

    /** Is there room on the deck? The driver's seat doesn't count: that one is kept for a player. */
    public boolean hasCrewSeat() {
        return deckRiders().size() < spec().layout().deckSeats().length;
    }

    private List<Entity> deckRiders() {
        LivingEntity driver = getControllingPassenger();
        return getPassengers().stream().filter(passenger -> passenger != driver).toList();
    }

    /** The first player aboard drives, whoever else climbed on before them. */
    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        for (Entity passenger : getPassengers()) {
            if (passenger instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        if (!hasPassenger(passenger)) {
            return;
        }
        Layout layout = spec().layout();
        double yawRad = Math.toRadians(getYRot());
        double fx = -Math.sin(yawRad);
        double fz = Math.cos(yawRad);
        if (passenger == getControllingPassenger()) {
            double[] seat = layout.driverSeat();
            callback.accept(passenger, getX() + fx * seat[0], getY() + seat[1] + passenger.getMyRidingOffset(), getZ() + fz * seat[0]);
            return;
        }
        double[] seat = layout.deckSeats()[Math.min(deckRiders().indexOf(passenger), layout.deckSeats().length - 1)];
        callback.accept(passenger, getX() + fx * seat[0] + fz * seat[1], getY() + seat[2] + passenger.getMyRidingOffset(),
                getZ() + fz * seat[0] - fx * seat[1]);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(spec().item().get());
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(spec().layout().cullMargin());
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_HEALTH, spec().maxHealth());
        entityData.define(DATA_FUEL, 0);
        entityData.define(DATA_TURRET, 0.0F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DATA_HEALTH, tag.contains("Health") ? tag.getFloat("Health") : spec().maxHealth());
        setFuel(tag.getInt(AircraftFuel.TAG));
        turretYaw = tag.getFloat("TurretYaw");
        entityData.set(DATA_TURRET, turretYaw);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Health", getHealth());
        tag.putInt(AircraftFuel.TAG, getFuel());
        tag.putFloat("TurretYaw", turretYaw);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
