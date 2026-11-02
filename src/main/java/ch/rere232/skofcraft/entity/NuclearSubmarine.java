package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.block.NuclearReactorBlock;
import ch.rere232.skofcraft.client.ApacheClientInput;
import ch.rere232.skofcraft.item.GigaIcbmItem;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.space.IcbmTargets;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * An Ohio-class ballistic missile submarine, with the real boat's proportions at 48 blocks long. W/S set the throttle,
 * which stays where you leave it, A/D work the rudder and Space/C blow and flood the ballast. It needs no fuel and the
 * crew breathe as long as they stay aboard. Attack pings the sonar, use fires a torpedo, and the launch key sends a
 * Giga ICBM out of a missile tube. Like the tanks, the captain's client simulates the movement.
 * <p>
 * The entity's own box is only the middle of the boat, and a box can't turn with the hull anyway, so blocks are found by
 * probing along the hull, and a row of {@link SubmarinePart}s is what you hit, click and stand on.
 */
public class NuclearSubmarine extends Entity implements PilotedAircraft {
    public static final int WEAPON_ICBM = 2;
    public static final double HALF_LENGTH = 24.0D;
    public static final double DECK_HEIGHT = 4.0D;
    /** How far above the keel the water comes when surfaced. */
    public static final double WATERLINE = 3.0D;
    public static final double SAIL_FORWARD = 12.5D;
    public static final double SAIL_TOP = 7.5D;
    public static final int MISSILE_TUBES = 16;
    public static final int NO_HATCH = -1;
    public static final int SONAR_CHART_TICKS = 120;
    public static final int MAX_FUEL = 1;
    public static final float MAX_HEALTH = 400.0F;

    private static final double MAX_SPEED = 0.5D;
    private static final double MAX_ASTERN = 0.15D;
    private static final double ACCELERATION = 0.005D;
    private static final double THROTTLE_RATE = 0.01D;
    private static final double MAX_VERTICAL = 0.12D;
    private static final double VERTICAL_ACCELERATION = 0.004D;
    private static final double SINK_RATE = 0.05D;
    private static final double ABANDONED_RISE = 0.03D;
    private static final float MAX_TURN = 0.8F;
    private static final double FULL_RUDDER_SPEED = 0.2D;
    private static final float MIN_RUDDER = 0.15F;
    private static final float ARMOR = 4.0F;
    private static final float RAMMING_DAMAGE = 25.0F;
    private static final double RAMMING_SPEED = 0.25D;
    private static final double STOPPED_DEAD = 0.02D;
    /** Position updates come in bursts, so one still tick means nothing. Stopping takes the engines about 100 ticks. */
    private static final int RAMMING_STOP_TICKS = 5;
    /** More than this in one tick can't be the engines, even with position updates arriving in bursts. */
    private static final double MAX_PLAUSIBLE_MOVE = 8.0D;
    private static final int PING_COOLDOWN = 100;
    private static final int TORPEDO_RELOAD = 160;
    private static final float MAX_TORPEDO_BEARING = 45.0F;
    private static final float MAX_TORPEDO_PITCH = 30.0F;
    /** The tubes only work when stopped and not too far down; the missile has to get out of the water. */
    private static final double MAX_LAUNCH_DEPTH = 20.0D;
    private static final double MAX_LAUNCH_SPEED = 0.05D;
    private static final int HATCH_OPEN_TICKS = GigaIcbm.COUNTDOWN + 60;
    private static final int HUM_INTERVAL = 40;
    private static final int MAX_CREW = 8;
    private static final int SURFACE_SCAN_LIMIT = 96;
    private static final double[][] SAIL_SEATS = {{13.5D, 0.0D}, {12.5D, 0.5D}, {12.5D, -0.5D}, {11.5D, 0.5D}, {11.5D, -0.5D}, {14.0D, 0.4D}, {14.0D, -0.4D}, {11.0D, 0.0D}};
    private static final double SEAT_HEIGHT = 7.1D;
    private static final int HULL_PARTS = 12;
    private static final float PART_SIZE = 4.0F;

    private static final EntityDataAccessor<Float> DATA_HEALTH = SynchedEntityData.defineId(NuclearSubmarine.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_MISSILES = SynchedEntityData.defineId(NuclearSubmarine.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_OPEN_HATCH = SynchedEntityData.defineId(NuclearSubmarine.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LAST_PING = SynchedEntityData.defineId(NuclearSubmarine.class, EntityDataSerializers.INT);

    private final SubmarinePart[] parts;
    private final List<ItemStack> missiles = new ArrayList<>();

    private double speed;
    private double vertical;
    private double throttle;
    private long nextPingTick;
    private long nextTorpedoTick;
    private int hatchTimer;
    private long lastExplosionTick = -1L;
    private boolean wasSubmerged;

    private double lastX;
    private double lastZ;
    private double speedBeforeStopping;
    private int stoppedTicks;

    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private float lerpYRot;

    /** Smoothed, since position updates come in bursts. */
    public float shownSpeed;
    /** Client only, for the model. */
    public float propAngle;
    public float propAngleO;
    public float hatchAngle;
    public float hatchAngleO;

    public NuclearSubmarine(EntityType<? extends NuclearSubmarine> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.blocksBuilding = true;
        this.parts = new SubmarinePart[HULL_PARTS + 1];
        for (int i = 0; i < HULL_PARTS; i++) {
            parts[i] = new SubmarinePart(this, PART_SIZE, PART_SIZE);
        }
        parts[HULL_PARTS] = new SubmarinePart(this, 2.0F, (float) (SAIL_TOP - DECK_HEIGHT));
        // The client works out each part's id from the parent's, so they have to be the ids straight after it
        setId(ENTITY_COUNTER.getAndAdd(parts.length + 1) + 1);
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        // Entity's constructor can't reach this: the array isn't there yet
        if (parts != null) {
            for (int i = 0; i < parts.length; i++) {
                parts[i].setId(id + i + 1);
            }
        }
    }

    public void place(double x, double y, double z, float yaw) {
        setPos(x, y, z);
        setYRot(yaw);
        xo = x;
        yo = y;
        zo = z;
        yRotO = yaw;
        lastX = x;
        lastZ = z;
        positionParts();
    }

    // ---- where things are ----

    private static Vec3 onHull(double x, double y, double z, float yaw, double forward, double left, double up) {
        double rad = Math.toRadians(yaw);
        double fx = -Math.sin(rad);
        double fz = Math.cos(rad);
        return new Vec3(x + fx * forward + fz * left, y + up, z + fz * forward - fx * left);
    }

    private Vec3 onHull(double forward, double left, double up) {
        return onHull(getX(), getY(), getZ(), getYRot(), forward, left, up);
    }

    private void positionParts() {
        for (int i = 0; i < HULL_PARTS; i++) {
            double forward = -HALF_LENGTH + PART_SIZE / 2.0D + i * PART_SIZE;
            parts[i].follow(onHull(forward, 0.0D, 0.0D));
        }
        parts[HULL_PARTS].follow(onHull(SAIL_FORWARD, 0.0D, DECK_HEIGHT));
    }

    /** Is the whole boat free of solid blocks, here and at this heading? Unloaded chunks count as solid. */
    public static boolean hullClear(Level level, double x, double y, double z, float yaw) {
        for (double forward = -HALF_LENGTH + 0.5D; forward <= HALF_LENGTH - 0.5D; forward += 3.0D) {
            boolean tapered = Math.abs(forward) > HALF_LENGTH - 5.0D;
            for (double left : tapered ? new double[] {0.0D} : new double[] {-1.5D, 1.5D}) {
                for (double up : new double[] {0.25D, 2.0D, DECK_HEIGHT - 0.25D}) {
                    if (solidAt(level, onHull(x, y, z, yaw, forward, left, up))) {
                        return false;
                    }
                }
            }
        }
        for (double up = DECK_HEIGHT + 0.5D; up <= SAIL_TOP; up += 1.5D) {
            if (solidAt(level, onHull(x, y, z, yaw, SAIL_FORWARD, 0.0D, up))) {
                return false;
            }
        }
        return true;
    }

    /** For launching a new boat: nothing in the way, and water all along the keel. */
    public static boolean canFloatAt(Level level, double x, double y, double z, float yaw) {
        if (!hullClear(level, x, y, z, yaw)) {
            return false;
        }
        for (double forward = -HALF_LENGTH + 0.5D; forward <= HALF_LENGTH - 0.5D; forward += 3.0D) {
            if (!level.getFluidState(BlockPos.containing(onHull(x, y, z, yaw, forward, 0.0D, 0.25D))).is(FluidTags.WATER)) {
                return false;
            }
        }
        return true;
    }

    private static boolean solidAt(Level level, Vec3 point) {
        BlockPos pos = BlockPos.containing(point);
        if (!level.isLoaded(pos)) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    /** The height of the first thing above the boat that isn't water, or NaN if the boat isn't in water at all. */
    private double surfaceAbove() {
        BlockPos.MutableBlockPos pos = BlockPos.containing(getX(), getY() + 0.5D, getZ()).mutable();
        if (!level().getFluidState(pos).is(FluidTags.WATER)) {
            return Double.NaN;
        }
        for (int i = 0; i < SURFACE_SCAN_LIMIT && level().getFluidState(pos).is(FluidTags.WATER); i++) {
            pos.move(0, 1, 0);
        }
        return pos.getY();
    }

    /** Blocks from the surface down to where the waterline would be when surfaced. Zero on the surface or out of water. */
    public double depth() {
        double surface = surfaceAbove();
        return Double.isNaN(surface) ? 0.0D : Math.max(0.0D, surface - (getY() + WATERLINE));
    }

    /** All the way under, sail included. Radar can't see it from here. */
    public boolean isSubmerged() {
        double surface = surfaceAbove();
        return !Double.isNaN(surface) && surface > getY() + SAIL_TOP;
    }

    // ---- sailing ----

    @Override
    public void tick() {
        super.tick();
        LivingEntity captain = getControllingPassenger();
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            sail(captain);
        } else {
            tickLerp();
        }
        positionParts();

        double dx = getX() - lastX;
        double dz = getZ() - lastZ;
        lastX = getX();
        lastZ = getZ();
        double moved = Math.sqrt(dx * dx + dz * dz);
        if (moved > MAX_PLAUSIBLE_MOVE) {
            // Just loaded or teleported, not sailing: lastX and lastZ start at zero on a boat read from disk
            moved = 0.0D;
        }
        shownSpeed += ((float) moved - shownSpeed) * 0.25F;

        for (Entity passenger : getPassengers()) {
            passenger.setAirSupply(passenger.getMaxAirSupply());
            passenger.resetFallDistance();
            passenger.clearFire();
        }
        if (level() instanceof ServerLevel serverLevel) {
            serverTick(serverLevel, captain, moved);
        } else {
            clientTick(moved);
        }
    }

    private void sail(@Nullable LivingEntity captain) {
        double surface = surfaceAbove();
        boolean afloat = !Double.isNaN(surface);
        float rudder = 0.0F;
        double wantedVertical;
        if (!afloat) {
            // Someone drained the ocean
            throttle = 0.0D;
            speed *= 0.8D;
            wantedVertical = -MAX_VERTICAL * 2.0D;
        } else if (captain != null) {
            throttle = Mth.clamp(throttle + Math.signum(captain.zza) * THROTTLE_RATE, -1.0D, 1.0D);
            rudder = captain.xxa;
            boolean up = Boolean.TRUE.equals(DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> ApacheClientInput.isJumping(captain)));
            boolean down = Boolean.TRUE.equals(DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> ApacheClientInput.isDescending(captain)));
            wantedVertical = up ? MAX_VERTICAL : down ? -MAX_VERTICAL : 0.0D;
        } else {
            // Nobody at the controls: all stop, and the ballast slowly blows so it can be found again
            throttle = 0.0D;
            wantedVertical = ABANDONED_RISE;
        }
        if (isLaunching()) {
            throttle = 0.0D;
        }
        double wantedSpeed = throttle >= 0.0D ? throttle * MAX_SPEED : throttle * MAX_ASTERN;
        speed += Mth.clamp(wantedSpeed - speed, -ACCELERATION, ACCELERATION);
        vertical += Mth.clamp(wantedVertical - vertical, -VERTICAL_ACCELERATION, VERTICAL_ACCELERATION);

        double y = getY();
        double nextY = y + vertical;
        if (afloat && nextY > surface - WATERLINE) {
            // Can't climb out of the water. Without this it would also ride up over the rim of a lake.
            nextY = Math.min(y, surface - WATERLINE);
            nextY = Math.max(nextY, y - SINK_RATE);
            vertical = 0.0D;
        }
        if (nextY != y && hullClear(level(), getX(), nextY, getZ(), getYRot())) {
            y = nextY;
        } else if (nextY != y) {
            vertical = 0.0D;
        }

        float nextYaw = getYRot();
        if (rudder != 0.0F) {
            float bite = Math.max(MIN_RUDDER, (float) Math.min(1.0D, Math.abs(speed) / FULL_RUDDER_SPEED));
            float turned = getYRot() - rudder * MAX_TURN * bite * (speed < 0.0D ? -1.0F : 1.0F);
            if (hullClear(level(), getX(), y, getZ(), turned)) {
                nextYaw = turned;
            }
        }

        double yawRad = Math.toRadians(nextYaw);
        double nextX = getX() - Math.sin(yawRad) * speed;
        double nextZ = getZ() + Math.cos(yawRad) * speed;
        if (speed == 0.0D || hullClear(level(), nextX, y, nextZ, nextYaw)) {
            setPos(nextX, y, nextZ);
        } else {
            speed = 0.0D;
            throttle = 0.0D;
            setPos(getX(), y, getZ());
        }
        setYRot(nextYaw);
    }

    private void serverTick(ServerLevel level, @Nullable LivingEntity captain, double moved) {
        // The captain's client drives, so a sudden stop is how the server finds out they hit something
        if (moved >= STOPPED_DEAD) {
            speedBeforeStopping = moved;
            stoppedTicks = 0;
        } else if (++stoppedTicks == RAMMING_STOP_TICKS && speedBeforeStopping > RAMMING_SPEED) {
            level.playSound(null, getX(), getY() + 2.0D, getZ(), SkofcraftSounds.HULL_CLANG.get(), SoundSource.NEUTRAL, 4.0F, 0.6F);
            damageHull(level.damageSources().generic(), RAMMING_DAMAGE);
            if (isRemoved()) {
                return;
            }
        }
        if (captain != null && tickCount % HUM_INTERVAL == 0) {
            level.playSound(null, getX(), getY() + 2.0D, getZ(), SkofcraftSounds.SUB_HUM.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        boolean submerged = isSubmerged();
        if (submerged && !wasSubmerged && captain != null) {
            level.playSound(null, getX(), getY() + 6.0D, getZ(), SkofcraftSounds.DIVE_KLAXON.get(), SoundSource.NEUTRAL, 2.0F, 1.0F);
        }
        wasSubmerged = submerged;
        if (hatchTimer > 0 && --hatchTimer == 0) {
            entityData.set(DATA_OPEN_HATCH, NO_HATCH);
        }
    }

    private void clientTick(double moved) {
        propAngleO = propAngle;
        propAngle += (float) (moved * 1.5D);
        hatchAngleO = hatchAngle;
        hatchAngle += Mth.clamp((isLaunching() ? 1.0F : 0.0F) - hatchAngle, -0.05F, 0.05F);
        if (moved > 0.05D && isInWater() && tickCount % 2 == 0) {
            Vec3 wake = onHull(-HALF_LENGTH - 1.0D, 0.0D, 2.0D);
            level().addParticle(ParticleTypes.BUBBLE, wake.x, wake.y, wake.z, 0.0D, 0.05D, 0.0D);
        }
    }

    // ---- weapons ----

    @Override
    public void fire(ServerPlayer captain, int weapon) {
        if (getControllingPassenger() != captain) {
            return;
        }
        ServerLevel level = captain.serverLevel();
        long now = level.getGameTime();
        if (weapon == WEAPON_GUN && now >= nextPingTick) {
            nextPingTick = now + PING_COOLDOWN;
            entityData.set(DATA_LAST_PING, (int) now);
            // Everyone nearby hears it too: an active ping gives away where you are
            level.playSound(null, getX(), getY() + 2.0D, getZ(), SkofcraftSounds.SONAR_PING.get(), SoundSource.NEUTRAL, 6.0F, 1.0F);
        } else if (weapon == WEAPON_MISSILE && now >= nextTorpedoTick) {
            fireTorpedo(level, captain, now);
        } else if (weapon == WEAPON_ICBM) {
            launchMissile(level, captain);
        }
    }

    private void fireTorpedo(ServerLevel level, ServerPlayer captain, long now) {
        Vec3 tube = onHull(HALF_LENGTH - 1.0D, 0.0D, 2.0D);
        if (!level.getFluidState(BlockPos.containing(tube)).is(FluidTags.WATER)) {
            return;
        }
        nextTorpedoTick = now + TORPEDO_RELOAD;
        float bearing = Mth.clamp(Mth.wrapDegrees(captain.getYRot() - getYRot()), -MAX_TORPEDO_BEARING, MAX_TORPEDO_BEARING);
        float pitch = Mth.clamp(captain.getXRot(), -MAX_TORPEDO_PITCH, MAX_TORPEDO_PITCH);
        double yaw = Math.toRadians(getYRot() + bearing);
        double pitchRad = Math.toRadians(pitch);
        Vec3 direction = new Vec3(-Math.sin(yaw) * Math.cos(pitchRad), -Math.sin(pitchRad), Math.cos(yaw) * Math.cos(pitchRad));
        level.addFreshEntity(new Torpedo(level, captain, this, tube, direction));
        level.playSound(null, tube.x, tube.y, tube.z, SkofcraftSounds.STEAM_HISS.get(), SoundSource.NEUTRAL, 3.0F, 0.7F);
        level.sendParticles(ParticleTypes.BUBBLE, tube.x, tube.y, tube.z, 30, 0.4D, 0.4D, 0.4D, 0.05D);
    }

    private void launchMissile(ServerLevel level, ServerPlayer captain) {
        if (isLaunching()) {
            return;
        }
        if (missiles.isEmpty()) {
            captain.displayClientMessage(Component.translatable("message.skofcraft.submarine_no_missiles").withStyle(ChatFormatting.RED), true);
            return;
        }
        ResourceLocation special = IcbmTargets.getSpecial(captain);
        BlockPos target = special != null ? null : IcbmTargets.get(captain);
        if (special == null && target == null) {
            captain.displayClientMessage(Component.translatable("message.skofcraft.submarine_no_target").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (shownSpeed > MAX_LAUNCH_SPEED || depth() > MAX_LAUNCH_DEPTH || Double.isNaN(surfaceAbove())) {
            captain.displayClientMessage(Component.translatable("message.skofcraft.submarine_launch_conditions").withStyle(ChatFormatting.RED), true);
            return;
        }
        int tube = missiles.size() - 1;
        Vec3 hatch = hatchPosition(tube);
        for (int y = (int) hatch.y; y < level.getMaxBuildHeight(); y++) {
            BlockPos pos = BlockPos.containing(hatch.x, y, hatch.z);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                captain.displayClientMessage(Component.translatable("message.skofcraft.submarine_launch_blocked").withStyle(ChatFormatting.RED), true);
                return;
            }
        }
        ItemStack missile = missiles.remove(tube);
        entityData.set(DATA_MISSILES, missiles.size());
        entityData.set(DATA_OPEN_HATCH, tube);
        hatchTimer = HATCH_OPEN_TICKS;
        float yieldScale = missile.getItem() instanceof GigaIcbmItem icbm ? icbm.yieldScale : 1.0F;
        GigaIcbm rocket = GigaIcbm.create(level, BlockPos.containing(hatch), 0.0D, target, special, captain.getUUID(), yieldScale);
        rocket.setPos(hatch.x, hatch.y, hatch.z);
        level.addFreshEntity(rocket);
        level.playSound(null, hatch.x, hatch.y, hatch.z, SkofcraftSounds.LAUNCH_ARM.get(), SoundSource.NEUTRAL, 3.0F, 0.8F);
        SkofcraftAdvancements.award(captain, SkofcraftAdvancements.SECOND_STRIKE);
        for (Entity passenger : getPassengers()) {
            if (passenger instanceof Player player) {
                player.displayClientMessage(Component.translatable("message.skofcraft.submarine_launching").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
            }
        }
    }

    /** Tubes count down the deck in two rows, starting behind the sail. */
    private Vec3 hatchPosition(int tube) {
        double forward = 9.0D - (tube / 2) * 1.75D;
        double left = tube % 2 == 0 ? 0.75D : -0.75D;
        return onHull(forward, left, DECK_HEIGHT + 0.4D);
    }

    public boolean isLaunching() {
        return getOpenHatch() != NO_HATCH;
    }

    public int getOpenHatch() {
        return entityData.get(DATA_OPEN_HATCH);
    }

    public int getMissileCount() {
        return entityData.get(DATA_MISSILES);
    }

    /** Game time of the last active ping. */
    public int getLastPing() {
        return entityData.get(DATA_LAST_PING);
    }

    public double getThrottle() {
        return throttle;
    }

    // ---- crew ----

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(SkofcraftItems.JET_FUEL_CANISTER.get())) {
            if (!level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.skofcraft.submarine_no_fuel"), true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (held.getItem() instanceof GigaIcbmItem) {
            if (!level().isClientSide) {
                loadMissile(player, held);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (player.isSecondaryUseActive() || !canAddPassenger(player)) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide && player.startRiding(this)) {
            player.displayClientMessage(Component.translatable("message.skofcraft.submarine_controls"), true);
            SkofcraftAdvancements.award(player, SkofcraftAdvancements.SILENT_SERVICE);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    private void loadMissile(Player player, ItemStack held) {
        if (missiles.size() >= MISSILE_TUBES) {
            player.displayClientMessage(Component.translatable("message.skofcraft.submarine_tubes_full"), true);
            return;
        }
        missiles.add(held.copyWithCount(1));
        entityData.set(DATA_MISSILES, missiles.size());
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        level().playSound(null, player.getX(), player.getY(), player.getZ(), SkofcraftSounds.PAYLOAD_LOAD.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.skofcraft.submarine_loaded", missiles.size(), MISSILE_TUBES), true);
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (!level().isClientSide && getControllingPassenger() == passenger) {
            long armed = level().getGameTime() + BOARDING_LOCKOUT_TICKS;
            nextPingTick = Math.max(nextPingTick, armed);
            nextTorpedoTick = Math.max(nextTorpedoTick, armed);
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        boolean wasCaptain = getControllingPassenger() == passenger;
        super.removePassenger(passenger);
        if (wasCaptain) {
            // The captain's client was driving, so the boat stops here. That isn't a collision, and the next
            // captain shouldn't inherit the old orders.
            speed = 0.0D;
            vertical = 0.0D;
            throttle = 0.0D;
            speedBeforeStopping = 0.0D;
            stoppedTicks = 0;
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && getPassengers().size() < MAX_CREW;
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
        double[] seat = SAIL_SEATS[Math.min(getPassengers().indexOf(passenger), SAIL_SEATS.length - 1)];
        Vec3 spot = onHull(seat[0], seat[1], SEAT_HEIGHT + passenger.getMyRidingOffset());
        callback.accept(passenger, spot.x, spot.y, spot.z);
        // The view turns with the boat
        float turned = getYRot() - yRotO;
        passenger.setYRot(passenger.getYRot() + turned);
        passenger.setYHeadRot(passenger.getYHeadRot() + turned);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return onHull(SAIL_FORWARD, 0.0D, SAIL_TOP + 0.1D);
    }

    @Override
    public boolean dismountsUnderwater() {
        return false;
    }

    @Override
    public boolean canBeRiddenUnderFluidType(net.minecraftforge.fluids.FluidType type, Entity rider) {
        return true;
    }

    /** Whoever is inside is behind the hull, whatever is happening out there. Damage is the hull's problem. */
    @Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class CrewProtection {
        private CrewProtection() {}

        @SubscribeEvent
        public static void onAttack(LivingAttackEvent event) {
            if (event.getEntity().getVehicle() instanceof NuclearSubmarine && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                event.setCanceled(true);
            }
        }
    }

    // ---- damage ----

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved() || isInvulnerableTo(source)
                || (source.getEntity() != null && hasPassenger(source.getEntity()))) {
            return false;
        }
        if (source.getDirectEntity() instanceof Player player && !isVehicle()) {
            if (!player.getAbilities().instabuild) {
                spawnAtLocation(new ItemStack(SkofcraftItems.NUCLEAR_SUBMARINE.get()));
                missiles.forEach(this::spawnAtLocation);
            }
            discard();
            return true;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            // The parts and the boat are all in the blast, and it only counts once
            long now = level().getGameTime();
            if (now == lastExplosionTick) {
                return false;
            }
            lastExplosionTick = now;
        }
        if (amount < ARMOR) {
            level().playSound(null, getX(), getY() + 2.0D, getZ(), SkofcraftSounds.HULL_CLANG.get(), SoundSource.NEUTRAL, 0.4F, 1.4F);
            return false;
        }
        damageHull(source, amount);
        return true;
    }

    private void damageHull(DamageSource source, float amount) {
        float remaining = getHealth() - amount;
        entityData.set(DATA_HEALTH, remaining);
        if (remaining > 0.0F || !(level() instanceof ServerLevel level)) {
            return;
        }
        AircraftWreck.awardCrew(this);
        ejectPassengers();
        BlockPos reactor = blockPosition().above(2);
        // Removed first: the blast would otherwise hurt this entity again and recurse.
        discard();
        level.explode(null, getX(), getY() + 2.0D, getZ(), 6.0F, Level.ExplosionInteraction.NONE);
        spawnAtLocation(new ItemStack(SkofcraftItems.AIRCRAFT_PLATE.get(), 12));
        spawnAtLocation(new ItemStack(SkofcraftItems.GEAR.get(), 8));
        // The reactor doesn't survive this, and neither does anything else nearby
        NuclearReactorBlock.scheduleMeltdown(level, reactor);
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
        return TORPEDO_RELOAD;
    }

    // ---- it's nuclear ----

    @Override
    public int getFuel() {
        return MAX_FUEL;
    }

    @Override
    public int getMaxFuel() {
        return MAX_FUEL;
    }

    @Override
    public void setFuel(int fuel) {
    }

    // ---- the entity plumbing ----

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpSteps = Math.max(1, steps);
    }

    private void tickLerp() {
        if (lerpSteps <= 0) {
            return;
        }
        double x = getX() + (lerpX - getX()) / lerpSteps;
        double y = getY() + (lerpY - getY()) / lerpSteps;
        double z = getZ() + (lerpZ - getZ()) / lerpSteps;
        setYRot(getYRot() + Mth.wrapDegrees(lerpYRot - getYRot()) / lerpSteps);
        setPos(x, y, z);
        lerpSteps--;
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return parts;
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return false;
    }

    /** You can only walk on the deck of a boat that isn't going anywhere. */
    public boolean isDeckSafe() {
        return !isVehicle() && shownSpeed < STOPPED_DEAD;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
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
    public ItemStack getPickResult() {
        return new ItemStack(SkofcraftItems.NUCLEAR_SUBMARINE.get());
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(HALF_LENGTH + 2.0D, 6.0D, HALF_LENGTH + 2.0D);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 512.0D * 512.0D;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_HEALTH, MAX_HEALTH);
        entityData.define(DATA_MISSILES, 0);
        entityData.define(DATA_OPEN_HATCH, NO_HATCH);
        entityData.define(DATA_LAST_PING, Integer.MIN_VALUE / 2);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DATA_HEALTH, tag.contains("Health") ? tag.getFloat("Health") : MAX_HEALTH);
        missiles.clear();
        ListTag list = tag.getList("Missiles", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            missiles.add(ItemStack.of(list.getCompound(i)));
        }
        entityData.set(DATA_MISSILES, missiles.size());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Health", getHealth());
        ListTag list = new ListTag();
        for (ItemStack missile : missiles) {
            list.add(missile.save(new CompoundTag()));
        }
        tag.put("Missiles", list);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
