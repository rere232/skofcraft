package ch.rere232.skofcraft.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** What you bring home from a proper stay in Pastel Paradise: two extra hearts and a better swing. */
public class WellRestedEffect extends MobEffect {
    public WellRestedEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF7B5D0);
        addAttributeModifier(Attributes.MAX_HEALTH, "2d9c6b14-7e30-4a8f-9b52-c1e6f0a3d728", 4.0D, AttributeModifier.Operation.ADDITION);
        addAttributeModifier(Attributes.ATTACK_SPEED, "81f3a7c2-5d16-49be-a04c-3b7e92d6f150", 0.1D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
