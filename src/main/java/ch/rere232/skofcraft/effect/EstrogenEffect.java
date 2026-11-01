package ch.rere232.skofcraft.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** From the Estrogen Pill: an extra heart and a lighter step. */
public class EstrogenEffect extends MobEffect {
    public EstrogenEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF5A9B8);
        addAttributeModifier(Attributes.MAX_HEALTH, "5c1e8a2f-3b7d-4e61-9a0c-7f2d4b8e6a13", 2.0D, AttributeModifier.Operation.ADDITION);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "a4f7c2d9-81e3-4b5a-b6d0-2e9c5f1a7b84", 0.1D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
