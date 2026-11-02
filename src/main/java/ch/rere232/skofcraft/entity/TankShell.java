package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.registry.SkofcraftEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;

/** The Maus's main gun round: fast, with a slight drop, and a big bang. Block damage follows the mob griefing rule. */
public class TankShell extends ThrowableItemProjectile {
    public static final float SPEED = 4.0F;
    private static final float POWER = 5.0F;
    private static final float GRAVITY = 0.01F;
    private static final int LIFETIME = 100;

    public TankShell(EntityType<? extends TankShell> type, Level level) {
        super(type, level);
    }

    public TankShell(Level level, LivingEntity owner) {
        super(SkofcraftEntities.TANK_SHELL.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.IRON_INGOT;
    }

    @Override
    protected float getGravity() {
        return GRAVITY;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        } else if (tickCount > LIFETIME) {
            discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            level().explode(this, getX(), getY(), getZ(), POWER, Level.ExplosionInteraction.MOB);
            discard();
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
