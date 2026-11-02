package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.hr.MandatoryHr;
import ch.rere232.skofcraft.registry.SkofcraftEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * A person in a lanyard who has come about your recent conduct. Walks to one particular player and starts their
 * training on arrival. Can't be hurt, and can't be outrun: too far away or too long on the way and they simply turn
 * up. Never saved with the world, because {@link MandatoryHr} sends another one if this one is ever gone.
 */
public class HrRepresentative extends PathfinderMob {
    private static final double ARRIVED_DISTANCE = 3.0D;
    private static final double TOO_FAR = 48.0D;
    private static final int GIVE_UP_WALKING_TICKS = 20 * 30;
    private static final int PATH_INTERVAL = 10;
    private static final double WALK_SPEED = 1.1D;

    private UUID owner;

    public HrRepresentative(EntityType<? extends HrRepresentative> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0D).add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    public void setOwner(ServerPlayer player) {
        owner = player.getUUID();
    }

    public boolean isFor(ServerPlayer player) {
        return player.getUUID().equals(owner);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
        if (player == null || player.level() != level) {
            discard();
            return;
        }
        if (MandatoryHr.isInTraining(player)) {
            getNavigation().stop();
            getLookControl().setLookAt(player);
            return;
        }
        if (!MandatoryHr.isDue(player)) {
            level.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0D, getZ(), 12, 0.2D, 0.5D, 0.2D, 0.01D);
            discard();
            return;
        }
        double distance = distanceTo(player);
        if (distance <= ARRIVED_DISTANCE || tickCount > GIVE_UP_WALKING_TICKS) {
            MandatoryHr.startTraining(player);
        } else if (distance > TOO_FAR) {
            teleportTo(player.getX() + 2.0D, player.getY(), player.getZ() + 2.0D);
        } else if (tickCount % PATH_INTERVAL == 0) {
            getNavigation().moveTo(player, WALK_SPEED);
        }
        getLookControl().setLookAt(player);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(source, amount);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    /**
     * Not called Registration: Forge names its generated event handlers after the simple class name, so that would
     * collide with {@link Femboy.Registration} in this package and call the wrong handler.
     */
    @Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class HrAttributes {
        private HrAttributes() {}

        @SubscribeEvent
        public static void onAttributes(EntityAttributeCreationEvent event) {
            event.put(SkofcraftEntities.HR_REPRESENTATIVE.get(), createAttributes().build());
        }
    }
}
