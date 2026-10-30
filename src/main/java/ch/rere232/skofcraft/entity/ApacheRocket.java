package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.registry.SkofcraftEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** An air-to-ground rocket that explodes on impact; block damage follows the mob griefing rule. */
public class ApacheRocket extends ThrowableItemProjectile {
    private static final float POWER = 2.5F;
    private static final int LIFETIME = 80;
    private static final double LOCK_RANGE = 48.0D;
    private static final double LOCK_CONE = 0.8D;
    private static final int HOMING_DELAY = 6;

    private boolean homing;
    private Entity target;

    public ApacheRocket(EntityType<? extends ApacheRocket> type, Level level) {
        super(type, level);
    }

    public ApacheRocket(Level level, LivingEntity owner) {
        super(SkofcraftEntities.APACHE_ROCKET.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.FIREWORK_ROCKET;
    }

    @Override
    protected float getGravity() {
        return 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        } else if (tickCount > LIFETIME) {
            discard();
        } else if (homing && tickCount >= HOMING_DELAY) {
            steer();
        }
    }

    public void setHoming(boolean homing) {
        this.homing = homing;
    }

    /** Locks the closest living entity inside a cone ahead, then bends toward it. */
    private void steer() {
        Vec3 motion = getDeltaMovement();
        double speed = motion.length();
        if (speed < 0.01D) {
            return;
        }
        if (target == null || !target.isAlive()) {
            target = findTarget(motion.scale(1.0D / speed));
            if (target == null) {
                return;
            }
        }
        Vec3 want = target.getBoundingBox().getCenter().subtract(position()).normalize();
        Vec3 bent = motion.scale(1.0D / speed).scale(0.8D).add(want.scale(0.2D)).normalize();
        setDeltaMovement(bent.scale(speed));
    }

    private Entity findTarget(Vec3 heading) {
        Entity owner = getOwner();
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(LOCK_RANGE))) {
            if (candidate == owner || !candidate.isAlive() || candidate.isSpectator()
                    || (owner != null && owner.getRootVehicle() == candidate.getRootVehicle())) {
                continue;
            }
            Vec3 toTarget = candidate.getBoundingBox().getCenter().subtract(position());
            double distance = toTarget.length();
            if (distance < 1.0D || toTarget.scale(1.0D / distance).dot(heading) < LOCK_CONE) {
                continue;
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
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
