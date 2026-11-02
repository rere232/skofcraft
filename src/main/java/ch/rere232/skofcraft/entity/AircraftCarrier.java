package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.carrier.CarrierBlocks;
import ch.rere232.skofcraft.carrier.CarrierLayout;
import ch.rere232.skofcraft.registry.SkofcraftEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A nuclear aircraft carrier underway. While anchored the carrier is ordinary blocks; taking the helm lifts
 * them into this entity, which carries everyone aboard as passengers at fixed spots on the ship. The captain
 * (first passenger) steers with W/S/A/D; Shift orders all stop, and once the ship has stopped and squared up
 * to the nearest 90 degrees the blocks go back into the world and the crew is set down where they stood.
 * Movement is simulated on the server only.
 */
public class AircraftCarrier extends Entity implements IEntityAdditionalSpawnData {
    public static final double MAX_SPEED = 0.5D;
    private static final double ASTERN_THROTTLE = -0.3D;
    private static final float MAX_TURN = 0.6F;
    private static final int AUTO_ANCHOR_TICKS = 40;
    /** Where the captain stands: behind the helm on the bridge. */
    private static final Vec3 CAPTAIN_SPOT = new Vec3(CarrierLayout.HELM.getX(), CarrierLayout.HELM.getY(), CarrierLayout.HELM.getZ() + 1);
    private static final Vec3 DEFAULT_SPOT = new Vec3(0.0D, CarrierLayout.DECK + 1, 0.0D);
    private static final List<Vec3> HULL_PROBES = hullProbes();
    private static final EntityDataAccessor<CompoundTag> DATA_CREW =
            SynchedEntityData.defineId(AircraftCarrier.class, EntityDataSerializers.COMPOUND_TAG);

    private CarrierBlocks blocks = new CarrierBlocks();
    /** Crew positions in the ship's frame, by UUID so they survive a save. */
    private final Map<UUID, Vec3> crewSpots = new HashMap<>();
    private double speed;
    private double throttle;
    private boolean anchoring;
    private boolean releasing;
    private int captainlessTicks;

    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private float lerpYRot;

    /** Client only: the baked mesh, owned by the renderer. */
    public Object renderCache;

    public AircraftCarrier(EntityType<? extends AircraftCarrier> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** Lifts the carrier anchored at {@code origin} out of the world and puts {@code captain} at the helm. */
    public static void getUnderway(ServerLevel level, BlockPos origin, Direction facing, CarrierBlocks blocks, ServerPlayer captain) {
        AircraftCarrier carrier = new AircraftCarrier(SkofcraftEntities.AIRCRAFT_CARRIER.get(), level);
        carrier.blocks = blocks;
        carrier.moveTo(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D, facing.toYRot(), 0.0F);

        List<Entity> aboard = level.getEntities((Entity) null, carrier.reach(), carrier::canTakeAboard);
        aboard.remove(captain);
        level.addFreshEntity(carrier);
        blocks.remove(level, origin, facing);

        carrier.takeAboard(captain, CAPTAIN_SPOT);
        for (Entity entity : aboard) {
            carrier.takeAboard(entity, carrier.toLocal(entity.position()));
        }
        captain.displayClientMessage(Component.translatable("message.skofcraft.carrier_controls"), true);
        level.playSound(null, captain.getX(), captain.getY(), captain.getZ(), SkofcraftSounds.SHIP_HORN.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
    }

    private AABB reach() {
        double radius = Math.max(-CarrierLayout.BOW_Z, CarrierLayout.STERN_Z) + 1.0D;
        return new AABB(getX() - radius, getY(), getZ() - radius, getX() + radius, getY() + CarrierLayout.HEIGHT, getZ() + radius);
    }

    private boolean canTakeAboard(Entity entity) {
        if (entity.isPassenger() || !entity.isAlive() || entity instanceof AircraftCarrier) {
            return false;
        }
        if (!(entity instanceof LivingEntity || entity instanceof PilotedAircraft || entity instanceof Boat || entity instanceof AbstractMinecart)) {
            return false;
        }
        Vec3 local = toLocal(entity.position());
        return Math.abs(local.x) <= CarrierLayout.HALF_WIDTH + 0.5D
                && local.z >= CarrierLayout.BOW_Z - 0.5D && local.z <= CarrierLayout.STERN_Z + 0.5D
                && local.y >= 0.0D && local.y <= CarrierLayout.HEIGHT;
    }

    private void takeAboard(Entity entity, Vec3 spot) {
        crewSpots.put(entity.getUUID(), spot);
        entity.startRiding(this, true);
    }

    // ---- frame conversion: the ship's frame has the bow toward -Z, see CarrierLayout ----

    private static Vec3 rotate(double x, double y, double z, float degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Vec3(x * cos + z * sin, y, -x * sin + z * cos);
    }

    /** Degrees to turn the ship's frame into the world, for the renderer too. */
    public static float frameRotation(float yaw) {
        return 180.0F - yaw;
    }

    private Vec3 toWorld(Vec3 local, Vec3 position, float yaw) {
        return rotate(local.x, local.y, local.z, frameRotation(yaw)).add(position);
    }

    private Vec3 toLocal(Vec3 world) {
        Vec3 d = world.subtract(position());
        return rotate(d.x, d.y, d.z, -frameRotation(getYRot()));
    }

    // ---- sailing ----

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            tickLerp();
            return;
        }
        LivingEntity captain = getControllingPassenger();
        captainlessTicks = captain == null ? captainlessTicks + 1 : 0;
        if (captainlessTicks > AUTO_ANCHOR_TICKS) {
            anchoring = true;
        }

        float turn = 0.0F;
        if (anchoring) {
            throttle = 0.0D;
            speed *= 0.92D;
            float square = Math.round(getYRot() / 90.0F) * 90.0F;
            float diff = Mth.wrapDegrees(square - getYRot());
            turn = Mth.clamp(diff, -MAX_TURN, MAX_TURN);
            if (Math.abs(speed) < 0.01D && Math.abs(diff) < 0.05F) {
                anchor(square);
                return;
            }
        } else if (captain != null) {
            if (captain.zza > 0.0F) {
                throttle = Math.min(1.0D, throttle + 0.01D);
            } else if (captain.zza < 0.0F) {
                throttle = Math.max(ASTERN_THROTTLE, throttle - 0.01D);
            }
            speed += (throttle * MAX_SPEED - speed) * 0.02D;
            // Rudder bites harder with way on, but she still swings slowly when stopped
            float bite = 0.35F + 0.65F * (float) Math.min(1.0D, Math.abs(speed) / MAX_SPEED);
            turn = -captain.xxa * MAX_TURN * bite;
            if (tickCount % 10 == 0 && captain instanceof Player player) {
                int knots = (int) Math.round(Math.abs(speed) * 20.0D * 1.944D);
                int heading = Math.floorMod(Math.round(getYRot() + 180.0F), 360);
                player.displayClientMessage(Component.translatable("message.skofcraft.carrier_status",
                        Math.round(throttle * 100.0D), knots, heading), true);
            }
        }
        steer(turn);
    }

    private void steer(float turn) {
        float yaw = getYRot() + turn;
        double rad = Math.toRadians(yaw);
        Vec3 next = position().add(-Math.sin(rad) * speed, 0.0D, Math.cos(rad) * speed);
        if (hullBlocked(next, yaw)) {
            if (Math.abs(speed) > 0.05D && getControllingPassenger() instanceof Player captain) {
                captain.displayClientMessage(Component.translatable("message.skofcraft.carrier_aground"), true);
                level().playSound(null, getX(), getY(), getZ(), SkofcraftSounds.HULL_CLANG.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            speed = 0.0D;
            throttle = 0.0D;
            return;
        }
        setYRot(yaw);
        setPos(next);
    }

    /** True if any part of the hull below the waterline would end up in solid ground. */
    private boolean hullBlocked(Vec3 position, float yaw) {
        for (Vec3 probe : HULL_PROBES) {
            Vec3 world = toWorld(probe, position, yaw);
            BlockPos pos = BlockPos.containing(world);
            if (!level().isLoaded(pos) || !level().getBlockState(pos).getCollisionShape(level(), pos).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static List<Vec3> hullProbes() {
        List<Vec3> probes = new ArrayList<>();
        for (int y = 0; y <= CarrierLayout.WATERLINE; y++) {
            for (int z = CarrierLayout.BOW_Z; z <= CarrierLayout.STERN_Z; z += 2) {
                int hw = CarrierLayout.hullHalfWidth(z);
                probes.add(new Vec3(-hw, y + 0.5D, z));
                probes.add(new Vec3(hw, y + 0.5D, z));
            }
            probes.add(new Vec3(0.0D, y + 0.5D, CarrierLayout.BOW_Z - 0.4D));
            probes.add(new Vec3(0.0D, y + 0.5D, CarrierLayout.STERN_Z));
        }
        return probes;
    }

    /** Puts the blocks back where the ship lies, squared to {@code yaw}, and sets everyone down where they stood. */
    private void anchor(float yaw) {
        Direction facing = Direction.fromYRot(yaw);
        BlockPos origin = BlockPos.containing(getX(), getY() + 0.5D, getZ());
        blocks.place(level(), origin, facing);

        Vec3 anchored = new Vec3(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        releasing = true;
        if (getControllingPassenger() instanceof Player captain) {
            captain.displayClientMessage(Component.translatable("message.skofcraft.carrier_anchored"), true);
        }
        for (Entity passenger : new ArrayList<>(getPassengers())) {
            Vec3 spot = toWorld(spotOf(passenger), anchored, yaw);
            passenger.stopRiding();
            passenger.teleportTo(spot.x, spot.y, spot.z);
        }
        level().playSound(null, getX(), getY() + CarrierLayout.DECK, getZ(), SkofcraftSounds.ANCHOR_CHAIN.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
        discard();
    }

    // ---- crew ----

    /** Server: whether the carrier should keep {@code entity} aboard when it tries to get off. */
    public boolean holdsAboard(Entity entity) {
        return !releasing && isAlive() && entity.isAlive() && !(entity instanceof ServerPlayer player && player.hasDisconnected());
    }

    /** Server: a passenger pressed Shift. The captain orders all stop; everyone else is told to wait. */
    public void onLeaveAttempt(Entity entity) {
        if (!(entity instanceof Player player)) {
            return;
        }
        if (entity == getControllingPassenger()) {
            if (!anchoring) {
                anchoring = true;
                player.displayClientMessage(Component.translatable("message.skofcraft.carrier_anchoring"), true);
            }
        } else {
            player.displayClientMessage(Component.translatable("message.skofcraft.carrier_underway"), true);
        }
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (!level().isClientSide) {
            // A player logging back in rejoins wherever they now are
            crewSpots.computeIfAbsent(passenger.getUUID(), id -> toLocal(passenger.position()));
            syncCrew();
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide) {
            crewSpots.remove(passenger.getUUID());
            syncCrew();
        }
    }

    private void syncCrew() {
        CompoundTag tag = new CompoundTag();
        for (Entity passenger : getPassengers()) {
            Vec3 spot = crewSpots.getOrDefault(passenger.getUUID(), DEFAULT_SPOT);
            CompoundTag entry = new CompoundTag();
            entry.putDouble("X", spot.x);
            entry.putDouble("Y", spot.y);
            entry.putDouble("Z", spot.z);
            tag.put(Integer.toString(passenger.getId()), entry);
        }
        entityData.set(DATA_CREW, tag);
    }

    private Vec3 spotOf(Entity passenger) {
        if (!level().isClientSide) {
            return crewSpots.getOrDefault(passenger.getUUID(), DEFAULT_SPOT);
        }
        CompoundTag entry = entityData.get(DATA_CREW).getCompound(Integer.toString(passenger.getId()));
        return entry.isEmpty() ? DEFAULT_SPOT : new Vec3(entry.getDouble("X"), entry.getDouble("Y"), entry.getDouble("Z"));
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        if (!hasPassenger(passenger)) {
            return;
        }
        Vec3 spot = toWorld(spotOf(passenger), position(), getYRot());
        callback.accept(passenger, spot.x, spot.y, spot.z);
        float turned = getYRot() - yRotO;
        passenger.setYRot(passenger.getYRot() + turned);
        passenger.setYHeadRot(passenger.getYHeadRot() + turned);
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return true;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player player ? player : null;
    }

    /** The server sails the ship; clients only interpolate, even for the captain. */
    @Override
    public boolean isControlledByLocalInstance() {
        return !level().isClientSide;
    }

    // ---- client interpolation ----

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

    // ---- misc entity behaviour ----

    public CarrierBlocks getBlocks() {
        return blocks;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
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
    public AABB getBoundingBoxForCulling() {
        return reach();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 512.0D * 512.0D;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_CREW, new CompoundTag());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        blocks = CarrierBlocks.load(level(), tag.getCompound("Blocks"));
        speed = tag.getDouble("Speed");
        throttle = tag.getDouble("Throttle");
        anchoring = tag.getBoolean("Anchoring");
        crewSpots.clear();
        ListTag crew = tag.getList("Crew", Tag.TAG_COMPOUND);
        for (int i = 0; i < crew.size(); i++) {
            CompoundTag entry = crew.getCompound(i);
            crewSpots.put(entry.getUUID("Id"), new Vec3(entry.getDouble("X"), entry.getDouble("Y"), entry.getDouble("Z")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Blocks", blocks.save());
        tag.putDouble("Speed", speed);
        tag.putDouble("Throttle", throttle);
        tag.putBoolean("Anchoring", anchoring);
        ListTag crew = new ListTag();
        crewSpots.forEach((id, spot) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", id);
            entry.putDouble("X", spot.x);
            entry.putDouble("Y", spot.y);
            entry.putDouble("Z", spot.z);
            crew.add(entry);
        });
        tag.put("Crew", crew);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        blocks.write(buffer);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        blocks = CarrierBlocks.read(buffer);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
