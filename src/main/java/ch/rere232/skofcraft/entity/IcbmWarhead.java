package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.registry.SkofcraftEntities;
import ch.rere232.skofcraft.space.GigaBlast;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

/** The Giga ICBM's re-entry vehicle, falling nose first out of the sky onto its target in a trail of dragon fire. */
public class IcbmWarhead extends Entity {
    public static final double SPAWN_ABOVE_BUILD_LIMIT = 60.0D;
    private static final double SPEED = 4.0D;
    private static final double VIEW_RANGE = 768.0D;

    private UUID owner = new UUID(0L, 0L);
    private float yieldScale = 1.0F;

    public IcbmWarhead(EntityType<? extends IcbmWarhead> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static IcbmWarhead create(ServerLevel level, double x, double z, UUID owner, float yieldScale) {
        IcbmWarhead warhead = new IcbmWarhead(SkofcraftEntities.ICBM_WARHEAD.get(), level);
        warhead.setPos(x, level.getMaxBuildHeight() + SPAWN_ABOVE_BUILD_LIMIT, z);
        warhead.owner = owner;
        warhead.yieldScale = yieldScale;
        return warhead;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0.0D, -SPEED, 0.0D);
        if (level().isClientSide) {
            trail();
            return;
        }
        ServerLevel level = (ServerLevel) level();
        Vec3 from = position();
        Vec3 to = from.add(0.0D, -SPEED, 0.0D);
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
        if (hit.getType() != HitResult.Type.MISS) {
            GigaBlast.detonate(level, hit.getBlockPos().above(), owner, yieldScale);
            discard();
            return;
        }
        if (to.y < level.getMinBuildHeight()) {
            GigaBlast.detonate(level, net.minecraft.core.BlockPos.containing(to.x, level.getMinBuildHeight() + 1, to.z), owner, yieldScale);
            discard();
            return;
        }
        setPos(to.x, to.y, to.z);
        if (tickCount % 5 == 0) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 8.0F, 0.5F);
        }
    }

    private void trail() {
        for (int i = 0; i < 14; i++) {
            double back = random.nextDouble() * SPEED * 2.0D;
            double spread = 0.4D + back * 0.15D;
            level().addAlwaysVisibleParticle(i % 3 == 0 ? ParticleTypes.DRAGON_BREATH : (i % 3 == 1 ? ParticleTypes.FLAME : ParticleTypes.LARGE_SMOKE), true,
                    getX() + (random.nextDouble() - 0.5D) * spread, getY() + 8.0D + back, getZ() + (random.nextDouble() - 0.5D) * spread,
                    0.0D, 0.05D, 0.0D);
        }
        level().addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, getX(), getY() + 10.0D, getZ(), 0.0D, 0.02D, 0.0D);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < VIEW_RANGE * VIEW_RANGE;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            owner = tag.getUUID("Owner");
        }
        yieldScale = tag.contains("Yield") ? tag.getFloat("Yield") : 1.0F;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putUUID("Owner", owner);
        tag.putFloat("Yield", yieldScale);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
