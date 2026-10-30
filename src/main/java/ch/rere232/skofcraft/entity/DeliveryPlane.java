package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.registry.SkofcraftEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

/** A carrier plane that flies over its target and releases a delivery crate. */
public class DeliveryPlane extends Entity {
    private static final double SPEED = 1.4D;
    private static final double START_DISTANCE = 70.0D;
    private static final double FLIGHT_HEIGHT = 32.0D;
    private static final double DROP_DISTANCE = 6.0D;
    private static final int MAX_AGE = 160;

    private UUID targetId;
    private ItemStack cargo = ItemStack.EMPTY;
    private boolean dropped;

    public DeliveryPlane(EntityType<? extends DeliveryPlane> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** Spawns a plane that approaches the player from a random direction. */
    public static void dispatch(ServerPlayer target, ItemStack cargo) {
        ServerLevel level = target.serverLevel();
        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        Vec3 direction = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
        double y = Math.min(target.getY() + FLIGHT_HEIGHT, level.getMaxBuildHeight() - 2);

        DeliveryPlane plane = new DeliveryPlane(SkofcraftEntities.DELIVERY_PLANE.get(), level);
        plane.targetId = target.getUUID();
        plane.cargo = cargo;
        plane.setPos(target.getX() - direction.x * START_DISTANCE, y, target.getZ() - direction.z * START_DISTANCE);
        plane.setDeltaMovement(direction.scale(SPEED));
        plane.faceMotion();
        level.addFreshEntity(plane);
    }

    private void faceMotion() {
        Vec3 motion = getDeltaMovement();
        setYRot((float) (Mth.atan2(-motion.x, motion.z) * 180.0D / Math.PI));
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        faceMotion();
        if (level().isClientSide) {
            return;
        }

        if (tickCount % 8 == 0) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.NEUTRAL, 3.0F, 0.6F);
        }
        if (!dropped) {
            tryDrop(motion);
        }
        if (tickCount > MAX_AGE) {
            discard();
        }
    }

    private void tryDrop(Vec3 motion) {
        Entity target = targetId == null ? null : ((ServerLevel) level()).getPlayerByUUID(targetId);
        double dx = target == null ? 0.0D : target.getX() - getX();
        double dz = target == null ? 0.0D : target.getZ() - getZ();
        boolean passed = dx * motion.x + dz * motion.z <= 0.0D;
        if (target == null || passed || Math.sqrt(dx * dx + dz * dz) < DROP_DISTANCE) {
            DeliveryCrate crate = new DeliveryCrate(SkofcraftEntities.DELIVERY_CRATE.get(), level());
            crate.setup(targetId, cargo);
            crate.setPos(getX(), getY() - 0.5D, getZ());
            level().addFreshEntity(crate);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.BARREL_OPEN, SoundSource.NEUTRAL, 2.0F, 1.0F);
            dropped = true;
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(4.0D);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        targetId = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
        cargo = ItemStack.of(tag.getCompound("Cargo"));
        dropped = tag.getBoolean("Dropped");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (targetId != null) {
            tag.putUUID("Target", targetId);
        }
        tag.put("Cargo", cargo.save(new CompoundTag()));
        tag.putBoolean("Dropped", dropped);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
