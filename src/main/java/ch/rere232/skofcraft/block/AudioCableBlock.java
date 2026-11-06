package ch.rere232.skofcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;

/**
 * Speaker cable. Joins up with other cables, DJ mixers and speakers on any side; a mixer plays through every speaker
 * its cables reach.
 */
public class AudioCableBlock extends PipeBlock {
    public AudioCableBlock(Properties properties) {
        super(0.125F, properties);
        BlockState state = this.stateDefinition.any();
        for (Direction direction : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), false);
        }
        this.registerDefaultState(state);
    }

    /** Cables, mixers and speakers are all part of an audio network. */
    public static boolean isAudioBlock(BlockState state) {
        Block block = state.getBlock();
        return block instanceof AudioCableBlock || block instanceof DjMixerBlock || block instanceof SpeakerBlock;
    }

    public BlockState withConnections(BlockGetter level, BlockPos pos) {
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), isAudioBlock(level.getBlockState(pos.relative(direction))));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(context.getLevel(), context.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), isAudioBlock(neighbor));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }
}
