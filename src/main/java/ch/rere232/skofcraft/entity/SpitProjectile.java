package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.registry.SkofcraftBlocks;
import ch.rere232.skofcraft.registry.SkofcraftEffects;
import ch.rere232.skofcraft.registry.SkofcraftEntities;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

/** A glob of spit: blinds what it hits, or leaves a puddle on the ground. */
public class SpitProjectile extends ThrowableItemProjectile {
    private static final int BLIND_TICKS = 60;
    private static final byte EVENT_SPLASH = 3;

    public SpitProjectile(EntityType<? extends SpitProjectile> type, Level level) {
        super(type, level);
    }

    public SpitProjectile(Level level, LivingEntity owner) {
        super(SkofcraftEntities.SPIT.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return SkofcraftItems.SPIT.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide && result.getEntity() instanceof LivingEntity target) {
            target.addEffect(new MobEffectInstance(SkofcraftEffects.SPIT_BLIND.get(), BLIND_TICKS, 0));
            if (target instanceof Mob mob) {
                mob.setTarget(null);
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level().isClientSide) {
            return;
        }
        BlockPos pos = result.getBlockPos().relative(result.getDirection());
        BlockState puddle = SkofcraftBlocks.SPIT_PUDDLE.get().defaultBlockState();
        if (level().getBlockState(pos).canBeReplaced() && puddle.canSurvive(level(), pos)) {
            level().setBlockAndUpdate(pos, puddle);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            level().broadcastEntityEvent(this, EVENT_SPLASH);
            discard();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_SPLASH) {
            for (int i = 0; i < 8; i++) {
                level().addParticle(new DustParticleOptions(new Vector3f(0.35F, 0.22F, 0.1F), 1.0F),
                        getX(), getY(), getZ(),
                        (random.nextDouble() - 0.5D) * 0.2D, random.nextDouble() * 0.15D, (random.nextDouble() - 0.5D) * 0.2D);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
