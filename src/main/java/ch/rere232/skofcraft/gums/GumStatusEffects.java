package ch.rere232.skofcraft.gums;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public class GumStatusEffects {
    private GumStatusEffects() {
    }

    public static void apply(Player player, int activeSlots, boolean enableNegativeEffects) {
        int positiveAmplifier = Math.min(1, activeSlots - 1);

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, positiveAmplifier, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120, positiveAmplifier, true, false, true));

        if (enableNegativeEffects && activeSlots >= 3) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, true, false, true));
        }
        if (enableNegativeEffects && activeSlots >= 4) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0, true, false, true));
        }
    }
}
