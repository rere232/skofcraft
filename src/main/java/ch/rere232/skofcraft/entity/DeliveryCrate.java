package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

/** A crate that falls out of a {@link DeliveryPlane}, steers onto its target's head and bursts open. */
public class DeliveryCrate extends Entity {
    private static final double GRAVITY = 0.05D;
    private static final double TERMINAL_SPEED = 1.4D;
    private static final float HEAD_DAMAGE = 2.0F;
    private static final int MAX_AGE = 400;

    private UUID targetId;
    private ItemStack cargo = ItemStack.EMPTY;

    public DeliveryCrate(EntityType<? extends DeliveryCrate> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    void setup(UUID targetId, ItemStack cargo) {
        this.targetId = targetId;
        this.cargo = cargo;
    }

    @Override
    public void tick() {
        super.tick();
        Player target = null;
        if (!level().isClientSide && targetId != null) {
            target = level().getPlayerByUUID(targetId);
            if (target != null && (!target.isAlive() || target.level() != level())) {
                target = null;
            }
        }

        Vec3 motion = getDeltaMovement();
        double vx = motion.x;
        double vz = motion.z;
        if (target != null) {
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            vx = dx * 0.3D;
            vz = dz * 0.3D;
            double speed = Math.sqrt(vx * vx + vz * vz);
            if (speed > 1.0D) {
                vx /= speed;
                vz /= speed;
            }
        }
        setDeltaMovement(vx, Math.max(motion.y - GRAVITY, -TERMINAL_SPEED), vz);
        Vec3 next = getDeltaMovement();
        setPos(getX() + next.x, getY() + next.y, getZ() + next.z);
        setYRot(getYRot() + 4.0F);

        if (!level().isClientSide && shouldLand(target)) {
            land(target);
        }
    }

    private boolean shouldLand(Player target) {
        if (tickCount > MAX_AGE || getY() < level().getMinBuildHeight()) {
            return true;
        }
        if (target != null) {
            return getY() <= target.getY() + target.getBbHeight() + 0.1D;
        }
        BlockPos pos = BlockPos.containing(getX(), getY() - 0.1D, getZ());
        return !level().getBlockState(pos).getCollisionShape(level(), pos).isEmpty();
    }

    private void land(Player target) {
        ServerLevel server = (ServerLevel) level();
        Vec3 at = target != null ? target.position().add(0.0D, target.getBbHeight(), 0.0D) : position();

        BlockState barrel = Blocks.BARREL.defaultBlockState();
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, barrel), at.x, at.y, at.z, 30, 0.3D, 0.2D, 0.3D, 0.1D);
        server.playSound(null, at.x, at.y, at.z, SkofcraftSounds.CRATE_LAND.get(), SoundSource.NEUTRAL, 0.6F, 1.0F);
        server.playSound(null, at.x, at.y, at.z, SkofcraftSounds.CRATE_OPEN.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);

        if (target != null) {
            float damage = Math.min(HEAD_DAMAGE, target.getHealth() - 1.0F);
            if (damage > 0.0F) {
                target.hurt(server.damageSources().fallingBlock(this), damage);
            }
            target.displayClientMessage(Component.translatable("phone.skofcraft.amazooon.delivered"), true);
        }

        ItemEntity drop = new ItemEntity(server, at.x, at.y, at.z, cargo.copy());
        drop.setDeltaMovement((random.nextDouble() - 0.5D) * 0.2D, 0.25D, (random.nextDouble() - 0.5D) * 0.2D);
        drop.setDefaultPickUpDelay();
        server.addFreshEntity(drop);
        discard();
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
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (targetId != null) {
            tag.putUUID("Target", targetId);
        }
        tag.put("Cargo", cargo.save(new CompoundTag()));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
