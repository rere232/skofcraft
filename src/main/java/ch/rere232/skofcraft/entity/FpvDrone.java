package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.client.FpvDroneClient;
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
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
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
 * A kamikaze quadcopter flown remotely. The pilot is not a passenger: their body stays where it is, defenceless, while
 * their camera lives on the drone. Their client simulates the flight and reports it in {@link #pilotUpdate}, the same
 * trust the helicopter gets through the vehicle move packet. With nobody on the link the server takes over and it drops.
 *
 * The client only has the chunks and entities around the pilot's body, so the link can't reach further than the
 * server sends them. That limit is the drone's signal range.
 */
public class FpvDrone extends Entity {
    public static final int FLAG_IMPACT = 1;
    public static final int FLAG_DETONATE = 2;
    public static final int FLAG_DISCONNECT = 4;
    public static final int FLAG_ON_GROUND = 8;

    /** Four minutes in the air, which is generous for a real one. */
    public static final int MAX_BATTERY = 20 * 60 * 4;
    /** The warhead stays safe this long after the link comes up, so launching next to a wall isn't suicide. */
    public static final int ARM_TICKS = 30;

    /** Thrust over (1 - drag) is the top speed: about 33 m/s along the nose. */
    private static final double THRUST = 0.075D;
    private static final double STRAFE = 0.04D;
    private static final double CLIMB = 0.045D;
    private static final double GRAVITY = 0.012D;
    private static final double DRAG = 0.955D;
    private static final double FREE_FALL = 0.04D;
    /** Touching something slower than this is a landing rather than an impact, so you can perch and wait. */
    private static final double IMPACT_SPEED = 0.45D;
    private static final double BLOCKED_AXIS = 1.0E-4D;

    /** The furthest one report may move it. Well above top speed, since reports can bunch up on a laggy link. */
    private static final double MAX_STEP = 6.0D;
    private static final int LINK_TIMEOUT_TICKS = 40;
    private static final int MIN_SIGNAL_CHUNKS = 2;
    private static final double SIGNAL_MARGIN = 8.0D;

    private static final float MAX_HEALTH = 6.0F;
    private static final float BLAST_POWER = 3.5F;
    /** On top of the blast, for whatever it flew into. Enough to beat any tank's armor. */
    private static final float DIRECT_DAMAGE = 30.0F;
    private static final double HIT_MARGIN = 0.3D;

    private static final EntityDataAccessor<Integer> DATA_PILOT =
            SynchedEntityData.defineId(FpvDrone.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BATTERY =
            SynchedEntityData.defineId(FpvDrone.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SIGNAL_RANGE =
            SynchedEntityData.defineId(FpvDrone.class, EntityDataSerializers.INT);

    /** What the pilot's hands are doing this tick. */
    public record Input(float forward, float strafe, float vertical, float yaw, float pitch) {
        static Input idle(Entity drone) {
            return new Input(0.0F, 0.0F, 0.0F, drone.getYRot(), drone.getXRot());
        }
    }

    private float health = MAX_HEALTH;
    private int flightTicks;
    private long lastReportTick;

    public boolean soundStarted;
    public float propAngle;
    public float propAngleO;
    public float tiltPitch;
    public float tiltPitchO;
    public float tiltRoll;
    public float tiltRollO;

    public FpvDrone(EntityType<? extends FpvDrone> type, Level level) {
        super(type, level);
    }

    public FpvDrone(Level level, double x, double y, double z, float yaw) {
        this(SkofcraftEntities.FPV_DRONE.get(), level);
        setPos(x, y, z);
        setYRot(yaw);
        xo = x;
        yo = y;
        zo = z;
    }

    @Override
    public void tick() {
        super.tick();
        flightTicks = isLinked() ? flightTicks + 1 : 0;
        if (level().isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FpvDroneClient.onDroneTick(this));
        } else if (isLinked()) {
            checkLink();
        }
        if (isControlledByLocalInstance()) {
            simulate();
        }
        resetFallDistance();
        animate();
    }

    private void simulate() {
        boolean pilotedHere = isLinked() && level().isClientSide;
        Input input = pilotedHere
                ? DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> FpvDroneClient.input(this))
                : Input.idle(this);
        boolean flooded = isInWater() || isInLava();
        if (isLinked() && getBattery() > 0 && !flooded) {
            fly(input);
        } else {
            Vec3 v = getDeltaMovement();
            setDeltaMovement(v.x * 0.98D, (v.y - FREE_FALL) * 0.98D, v.z * 0.98D);
        }
        Vec3 wanted = getDeltaMovement();
        Vec3 start = position();
        move(MoverType.SELF, wanted);
        if (verticalCollision) {
            setDeltaMovement(getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
        }
        if (pilotedHere) {
            boolean impact = speedLostTo(wanted, position().subtract(start)) >= IMPACT_SPEED || flooded;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FpvDroneClient.report(this, impact, flooded));
        }
    }

    /** How fast it was going into whatever stopped it, per blocked axis. Skimming along the ground doesn't count. */
    private static double speedLostTo(Vec3 wanted, Vec3 moved) {
        double x = Math.abs(wanted.x - moved.x) > BLOCKED_AXIS ? wanted.x : 0.0D;
        double y = Math.abs(wanted.y - moved.y) > BLOCKED_AXIS ? wanted.y : 0.0D;
        double z = Math.abs(wanted.z - moved.z) > BLOCKED_AXIS ? wanted.z : 0.0D;
        return Math.sqrt(x * x + y * y + z * z);
    }

    private void fly(Input input) {
        setYRot(input.yaw());
        setXRot(input.pitch());
        double yawRad = Math.toRadians(getYRot());
        Vec3 left = new Vec3(Math.cos(yawRad), 0.0D, Math.sin(yawRad));
        // Any stick input holds it up, so it goes where the nose points. Let go and it sinks.
        boolean handsOff = input.forward() == 0.0F && input.strafe() == 0.0F && input.vertical() == 0.0F;
        Vec3 v = getDeltaMovement()
                .add(getLookAngle().scale(input.forward() * THRUST))
                .add(left.scale(input.strafe() * STRAFE))
                .add(0.0D, input.vertical() * CLIMB - (handsOff ? GRAVITY : 0.0D), 0.0D)
                .scale(DRAG);
        if (getY() >= level().getMaxBuildHeight() - 1 && v.y > 0.0D) {
            v = new Vec3(v.x, 0.0D, v.z);
        }
        setDeltaMovement(v);
    }

    private void animate() {
        propAngleO = propAngle;
        boolean spinning = isLinked() && getBattery() > 0;
        propAngle += spinning ? (onGround() ? 25.0F : 75.0F) : 0.0F;

        double yawRad = Math.toRadians(getYRot());
        double dx = getX() - xo;
        double dz = getZ() - zo;
        double forwardSpeed = -Math.sin(yawRad) * dx + Math.cos(yawRad) * dz;
        double leftSpeed = Math.cos(yawRad) * dx + Math.sin(yawRad) * dz;
        tiltPitchO = tiltPitch;
        tiltRollO = tiltRoll;
        tiltPitch += (Mth.clamp((float) forwardSpeed * 30.0F, -35.0F, 35.0F) - tiltPitch) * 0.3F;
        tiltRoll += (Mth.clamp((float) leftSpeed * 40.0F, -35.0F, 35.0F) - tiltRoll) * 0.3F;
    }

    /** The server's watch over a live link. Dropping it leaves the drone to fall. */
    private void checkLink() {
        Player pilot = getPilot();
        boolean timedOut = level().getGameTime() - lastReportTick > LINK_TIMEOUT_TICKS;
        if (pilot == null || !pilot.isAlive() || pilot.isPassenger() || timedOut
                || horizontalDistanceTo(pilot) > getSignalRange() + SIGNAL_MARGIN) {
            unlink();
        } else if (!onGround() && getBattery() > 0) {
            entityData.set(DATA_BATTERY, getBattery() - 1);
        }
    }

    public double horizontalDistanceTo(Entity other) {
        return Math.hypot(getX() - other.getX(), getZ() - other.getZ());
    }

    /** Puts the player on the link. The server only sends chunks and ticks entities so far from their body. */
    public void link(ServerPlayer player) {
        var players = player.server.getPlayerList();
        int chunks = Math.max(MIN_SIGNAL_CHUNKS, Math.min(players.getViewDistance(), players.getSimulationDistance()) - 1);
        entityData.set(DATA_SIGNAL_RANGE, chunks * 16);
        entityData.set(DATA_PILOT, player.getId());
        flightTicks = 0;
        lastReportTick = level().getGameTime();
        player.displayClientMessage(Component.translatable("message.skofcraft.fpv_controls"), true);
    }

    public void unlink() {
        entityData.set(DATA_PILOT, -1);
    }

    /** Why this player can't take the link right now, or null if they can. */
    @Nullable
    public static Component whyNotPilot(Player player) {
        if (player.isPassenger()) {
            return Component.translatable("message.skofcraft.fpv_in_vehicle");
        }
        // Their position stops updating while they fly, and the server kicks anyone it thinks is hanging in mid-air
        if (!player.onGround() && !player.getAbilities().mayfly) {
            return Component.translatable("message.skofcraft.fpv_not_grounded");
        }
        return null;
    }

    /** A flight report from the pilot's client. */
    public void pilotUpdate(ServerPlayer sender, double x, double y, double z, float yaw, float pitch, int flags) {
        if (isRemoved() || getPilotId() != sender.getId()) {
            return;
        }
        lastReportTick = level().getGameTime();
        Vec3 from = position();
        Vec3 step = new Vec3(x, y, z).subtract(from);
        if (step.length() > MAX_STEP) {
            step = step.normalize().scale(MAX_STEP);
        }
        // Moved through collision instead of placed, so a hacked client can't fly it through walls
        move(MoverType.SELF, step);
        setDeltaMovement(step);
        setYRot(Mth.wrapDegrees(yaw));
        setXRot(Mth.clamp(pitch, -90.0F, 90.0F));
        setOnGround((flags & FLAG_ON_GROUND) != 0);

        if ((flags & FLAG_DISCONNECT) != 0) {
            unlink();
            return;
        }
        if (!isArmed()) {
            return;
        }
        Entity struck = findStruck(from, position());
        if (struck != null || (flags & (FLAG_IMPACT | FLAG_DETONATE)) != 0) {
            detonate(struck);
        }
    }

    /** The first thing shootable on the way from one point to the other. */
    @Nullable
    private Entity findStruck(Vec3 from, Vec3 to) {
        AABB sweep = getBoundingBox().expandTowards(from.subtract(to)).inflate(HIT_MARGIN);
        Entity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entity entity : level().getEntities(this, sweep, EntitySelector.NO_SPECTATORS.and(Entity::isPickable))) {
            AABB box = entity.getBoundingBox().inflate(getBbWidth() / 2.0D + HIT_MARGIN);
            if (box.contains(to) || box.clip(from, to).isPresent()) {
                double distance = entity.distanceToSqr(from);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closest = entity;
                }
            }
        }
        return closest;
    }

    private void detonate(@Nullable Entity struck) {
        Player pilot = getPilot();
        DamageSource source = damageSources().explosion(this, pilot);
        if (struck != null) {
            struck.hurt(source, DIRECT_DAMAGE);
            if (struck instanceof Tank && pilot != null) {
                SkofcraftAdvancements.award(pilot, SkofcraftAdvancements.COPE_CAGE);
            }
        }
        double x = getX();
        double y = getY();
        double z = getZ();
        // Removed first: the blast would otherwise hurt this entity again and recurse.
        discard();
        level().explode(this, source, null, x, y, z, BLAST_POWER, false, Level.ExplosionInteraction.MOB);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved() || isInvulnerableTo(source)) {
            return false;
        }
        if (!isLinked() && source.getDirectEntity() instanceof Player player) {
            if (!player.getAbilities().instabuild) {
                spawnAtLocation(new ItemStack(SkofcraftItems.FPV_DRONE.get()));
            }
            discard();
            return true;
        }
        health -= amount;
        if (health <= 0.0F) {
            if (isArmed()) {
                detonate(null);
            } else {
                discard();
                level().explode(null, getX(), getY(), getZ(), 0.5F, Level.ExplosionInteraction.NONE);
            }
        }
        return true;
    }

    /** Landed drones can be picked back up on the link for as long as the battery lasts. */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (isLinked()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            Component problem = getBattery() <= 0 ? Component.translatable("message.skofcraft.fpv_flat_battery") : whyNotPilot(player);
            if (problem != null) {
                player.displayClientMessage(problem, true);
            } else {
                link(serverPlayer);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public int getPilotId() {
        return entityData.get(DATA_PILOT);
    }

    public boolean isLinked() {
        return getPilotId() >= 0;
    }

    @Nullable
    public Player getPilot() {
        return isLinked() && level().getEntity(getPilotId()) instanceof Player player ? player : null;
    }

    public boolean isArmed() {
        return isLinked() && flightTicks >= ARM_TICKS;
    }

    public int getBattery() {
        return entityData.get(DATA_BATTERY);
    }

    public int getSignalRange() {
        return entityData.get(DATA_SIGNAL_RANGE);
    }

    public int getFlightTicks() {
        return flightTicks;
    }

    /** On the pilot's client the flight is simulated there; everywhere else the server has it, unless the link is up. */
    @Override
    public boolean isControlledByLocalInstance() {
        if (level().isClientSide) {
            return Boolean.TRUE.equals(DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> FpvDroneClient.isLocalPilot(this)));
        }
        return !isLinked();
    }

    /** The pilot's client knows better than the server's echo of what it reported a moment ago. */
    @Override
    public void lerpMotion(double x, double y, double z) {
        if (!isControlledByLocalInstance()) {
            super.lerpMotion(x, y, z);
        }
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
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128.0D * 128.0D;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(SkofcraftItems.FPV_DRONE.get());
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_PILOT, -1);
        entityData.define(DATA_BATTERY, MAX_BATTERY);
        entityData.define(DATA_SIGNAL_RANGE, MIN_SIGNAL_CHUNKS * 16);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        health = tag.contains("Health") ? tag.getFloat("Health") : MAX_HEALTH;
        entityData.set(DATA_BATTERY, tag.contains("Battery") ? Mth.clamp(tag.getInt("Battery"), 0, MAX_BATTERY) : MAX_BATTERY);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Health", health);
        tag.putInt("Battery", getBattery());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
