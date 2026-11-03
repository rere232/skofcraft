package ch.rere232.skofcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.function.Supplier;

/** A river of SKOFee. Swimming in it counts as having a cup, in the sense that you absorb it. */
public class SkofeeFluidBlock extends LiquidBlock {
    private static final int EFFECT_TICKS = 100;

    public SkofeeFluidBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living && entity.tickCount % 20 == 0) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, EFFECT_TICKS, 0, true, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, EFFECT_TICKS, 0, true, false, true));
        }
    }
}
