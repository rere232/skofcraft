package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.registry.SkofcraftEntities;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Panzer VIII Maus: slow, enormous and very hard to hurt. W/S drive, A/D pivot the hull, and the turret slowly
 * follows where the driver looks. Attack fires the machine gun, use fires the main gun, and both go where the barrel
 * is pointing, not where you're looking. It rolls over small blocks and whoever is standing in the way. Like the
 * aircraft, the driving client simulates the movement and the server accepts it through the vehicle move packet.
 */
public class MausTank extends Entity implements PilotedAircraft {
    public static final float MAX_HEALTH = 200.0F;
    public static final int MAX_FUEL = AircraftFuel.PER_CANISTER * 3;
    public static final int RELOAD_TICKS = 20 * 8;
    /** Top speed in blocks per tick, about the 20 km/h the real one managed. */
    public static final double MAX_SPEED = 0.28D;

    private static final double MAX_REVERSE = 0.12D;
    private static final double ACCELERATION = 0.006D;
    private static final double BRAKING = 0.02D;
    private static final float HULL_TURN_RATE = 1.5F;
    private static final float TURRET_TURN_RATE = 2.5F;
    private static final float GUN_MAX_UP = -20.0F;
    private static final float GUN_MAX_DOWN = 10.0F;
    private static final int GUN_COOLDOWN = 3;
    /** Anything weaker than this just bounces off the armor. */
    private static final float ARMOR = 4.0F;
    /** Where the gun pivots, relative to the tank, and how far out its muzzle is from there. */
    private static final double GUN_PIVOT_FORWARD = 0.5D;
    private static final double GUN_PIVOT_HEIGHT = 3.0D;
    private static final double MUZZLE_DISTANCE = 7.2D;
    /** Forward and height of the driver, head out of the turret hatch. Only ever a player. */
    private static final double[] DRIVER_SEAT = {0.5D, 2.75D};
    /** Everyone else, as forward, left, height: four on the engine deck and two on the hull in front of the turret. */
    private static final double[][] DECK_SEATS = {
            {-2.0D, 0.7D, 2.0D}, {-2.0D, -0.7D, 2.0D},
            {-3.2D, 0.7D, 2.0D}, {-3.2D, -0.7D, 2.0D},
            {2.8D, 1.0D, 1.65D}, {2.8D, -1.0D, 1.65D}
    };
    private static final double CRUSH_FORWARD = 4.0D;
    private static final double CRUSH_REACH = 2.2D;
    private static final float CRUSH_DAMAGE = 10.0F;
    private static final float CRUSHABLE_HARDNESS = 0.6F;
    private static final double MIN_CRUSH_SPEED = 0.05D;
    private static final int ENGINE_INTERVAL = 10;

    private static final EntityDataAccessor<Float> DATA_HEALTH = SynchedEntityData.defineId(MausTank.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_FUEL = SynchedEntityData.defineId(MausTank.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_TURRET = SynchedEntityData.defineId(MausTank.class, EntityDataSerializers.FLOAT);

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

    public MausTank(EntityType<? extends MausTank> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        setMaxUpStep(1.1F);
    }

    public MausTank(Level level, double x, double y, double z, float yaw) {
        this(SkofcraftEntities.MAUS.get(), level);
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
                serverLevel.playSound(null, getX(), getY(), getZ(), SkofcraftSounds.TANK_ENGINE.get(), SoundSource.NEUTRAL, 2.0F, 1.0F);
            }
            if (moved > MIN_CRUSH_SPEED && tickCount % 2 == 0) {
                crush(serverLevel, driver, dx, dz, moved);
            }
        }
    }

    private void drive(LivingEntity driver) {
        if (driver.zza > 0.0F) {
            speed = Math.min(MAX_SPEED, speed + (speed < 0.0D ? BRAKING : ACCELERATION));
        } else if (driver.zza < 0.0F) {
            speed = Math.max(-MAX_REVERSE, speed - (speed > 0.0D ? BRAKING : ACCELERATION));
        } else {
            speed *= 0.97D;
        }
        // Tracks can pivot on the spot, which a bus can't
        setYRot(getYRot() - driver.xxa * HULL_TURN_RATE);
    }

    /** Both sides work this out from the driver's view, and without a driver clients take the server's word for it. */
    private void aimTurret(@Nullable LivingEntity driver, boolean running) {
        turretYawO = turretYaw;
        gunPitchO = gunPitch;
        if (running) {
            turretYaw += Mth.clamp(Mth.wrapDegrees(driver.getYRot() - turretYaw), -TURRET_TURN_RATE, TURRET_TURN_RATE);
            float wantedPitch = Mth.clamp(driver.getXRot(), GUN_MAX_UP, GUN_MAX_DOWN);
            gunPitch += Mth.clamp(wantedPitch - gunPitch, -TURRET_TURN_RATE, TURRET_TURN_RATE);
        } else if (level().isClientSide) {
            turretYaw = entityData.get(DATA_TURRET);
        }
        if (!level().isClientSide) {
            entityData.set(DATA_TURRET, turretYaw);
        }
    }

    /** Flattens whatever is in front: anything soft, and fences and doors, if griefing is allowed. Mobs either way. */
    private void crush(ServerLevel level, LivingEntity driver, double dx, double dz, double moved) {
        Vec3 heading = new Vec3(dx / moved, 0.0D, dz / moved);
        Vec3 front = position().add(heading.scale(CRUSH_FORWARD));
        AABB zone = new AABB(front, front).inflate(CRUSH_REACH, 0.0D, CRUSH_REACH).expandTowards(0.0D, 3.0D, 0.0D);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, zone, entity -> !hasPassenger(entity) && !entity.isSpectator() && !(entity instanceof AnimeCrew))) {
            victim.hurt(driver instanceof Player player ? level.damageSources().playerAttack(player) : level.damageSources().generic(), CRUSH_DAMAGE);
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
        double hullYaw = Math.toRadians(getYRot());
        Vec3 pivot = new Vec3(getX() - Math.sin(hullYaw) * GUN_PIVOT_FORWARD, getY() + GUN_PIVOT_HEIGHT, getZ() + Math.cos(hullYaw) * GUN_PIVOT_FORWARD);
        return pivot.add(direction.scale(MUZZLE_DISTANCE));
    }

    /** Called on the server for the driver's fire packets. */
    @Override
    public void fire(ServerPlayer driver, int weapon) {
        if (getControllingPassenger() != driver) {
            return;
        }
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
            nextCannonTick = now + RELOAD_TICKS;
            TankShell shell = new TankShell(level(), driver);
            shell.setPos(muzzle.x, muzzle.y, muzzle.z);
            shell.shoot(direction.x, direction.y, direction.z, TankShell.SPEED, 0.0F);
            level().addFreshEntity(shell);
            level().playSound(null, muzzle.x, muzzle.y, muzzle.z, SkofcraftSounds.TANK_CANNON.get(), SoundSource.NEUTRAL, 6.0F, 1.0F);
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
            SkofcraftAdvancements.award(passenger, SkofcraftAdvancements.ONE_HUNDRED_EIGHTY_EIGHT_TONS);
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
                spawnAtLocation(AircraftFuel.withFuel(new ItemStack(SkofcraftItems.MAUS.get()), getFuel()));
            }
            discard();
            return true;
        }
        if (amount < ARMOR) {
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
        return MAX_HEALTH;
    }

    @Override
    public int secondaryCooldownTicks() {
        return RELOAD_TICKS;
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
        return deckRiders().size() < DECK_SEATS.length;
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
        double yawRad = Math.toRadians(getYRot());
        double fx = -Math.sin(yawRad);
        double fz = Math.cos(yawRad);
        if (passenger == getControllingPassenger()) {
            callback.accept(passenger, getX() + fx * DRIVER_SEAT[0], getY() + DRIVER_SEAT[1] + passenger.getMyRidingOffset(), getZ() + fz * DRIVER_SEAT[0]);
            return;
        }
        double[] seat = DECK_SEATS[Math.min(deckRiders().indexOf(passenger), DECK_SEATS.length - 1)];
        callback.accept(passenger, getX() + fx * seat[0] + fz * seat[1], getY() + seat[2] + passenger.getMyRidingOffset(),
                getZ() + fz * seat[0] - fx * seat[1]);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(SkofcraftItems.MAUS.get());
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(9.0D);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_HEALTH, MAX_HEALTH);
        entityData.define(DATA_FUEL, 0);
        entityData.define(DATA_TURRET, 0.0F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DATA_HEALTH, tag.contains("Health") ? tag.getFloat("Health") : MAX_HEALTH);
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
