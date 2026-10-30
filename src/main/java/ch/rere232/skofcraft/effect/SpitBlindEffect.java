package ch.rere232.skofcraft.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Brown spit in the eyes: players get a brown overlay, mobs lose their target. */
public class SpitBlindEffect extends MobEffect {
    public SpitBlindEffect() {
        super(MobEffectCategory.HARMFUL, 0x5A3A1C);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
