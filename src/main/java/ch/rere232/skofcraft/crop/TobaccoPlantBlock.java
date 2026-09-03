package ch.rere232.skofcraft.crop;

import ch.rere232.skofcraft.registry.SkofcraftItems;
import ch.rere232.skofcraft.config.SkofcraftConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class TobaccoPlantBlock extends CropBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 7);

    public TobaccoPlantBlock(Block.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return SkofcraftItems.TOBACCO_SEEDS.get();
    }

    @Override
    public IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return 7;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        double growthSpeed = Math.max(0.1D, SkofcraftConfig.tobaccoGrowthSpeed);
        int attempts = (int) growthSpeed;
        if (random.nextDouble() < growthSpeed - attempts) {
            attempts++;
        }
        for (int attempt = 0; attempt < Math.max(1, attempts); attempt++) {
            super.randomTick(state, level, pos, random);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }
}
