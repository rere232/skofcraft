package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.phone.PhoneAddiction;
import ch.rere232.skofcraft.registry.SkofcraftEntities;
import ch.rere232.skofcraft.registry.SkofcraftSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * What a bad phone habit looks like once it can walk. Paradise is safe until you bring one in: these only turn up around
 * players with a high phone addiction, and their hits make it worse.
 */
public class Gloom extends Monster {
    private static final int HIT_ADDICTION = 2;

    public Gloom(EntityType<? extends Gloom> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 16.0D).add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D).add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof ServerPlayer player) {
            PhoneAddiction.add(player, HIT_ADDICTION);
            playSound(SkofcraftSounds.PHONE_NOTIFY.get(), 1.0F, 0.7F);
        }
        return hit;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SkofcraftSounds.PHONE_NOTIFY.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    /** There is nothing under the islands, and the void would otherwise take its loot with it. */
    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && getY() < level().getMinBuildHeight()) {
            discard();
        }
    }

    @Mod.EventBusSubscriber(modid = ch.rere232.skofcraft.SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Events {
        @SubscribeEvent
        public static void onAttributes(EntityAttributeCreationEvent event) {
            event.put(SkofcraftEntities.GLOOM.get(), createAttributes().build());
            event.put(SkofcraftEntities.THE_ALGORITHM.get(), TheAlgorithm.createAlgorithmAttributes().build());
            event.put(SkofcraftEntities.BOUTIQUE_KEEPER.get(), KingdomNoble.createAttributes().build());
        }
    }

    static Mob create(Level level) {
        return SkofcraftEntities.GLOOM.get().create(level);
    }
}
