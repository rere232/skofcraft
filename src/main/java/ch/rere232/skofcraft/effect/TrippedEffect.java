package ch.rere232.skofcraft.effect;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.registry.SkofcraftEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** From the Evil Tripwire: flat on your face, crawling at a fraction of your speed and unable to jump. */
public class TrippedEffect extends MobEffect {
    public TrippedEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B1A1A);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "0d3f6b7e-52a9-4c18-8e3b-6a1f9d2c7e40", -0.6D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Events {
        private Events() {}

        /** The player's pose is worked out again every tick, so this has to come after that, every tick. */
        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase == TickEvent.Phase.END && event.player.hasEffect(SkofcraftEffects.TRIPPED.get())) {
                event.player.setSprinting(false);
                event.player.setPose(Pose.SWIMMING);
            }
        }

        @SubscribeEvent
        public static void onJump(LivingEvent.LivingJumpEvent event) {
            if (event.getEntity().hasEffect(SkofcraftEffects.TRIPPED.get())) {
                event.getEntity().setDeltaMovement(event.getEntity().getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
            }
        }
    }
}
