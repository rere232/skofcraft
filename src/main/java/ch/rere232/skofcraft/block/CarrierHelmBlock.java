package ch.rere232.skofcraft.block;

import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.carrier.CarrierBlocks;
import ch.rere232.skofcraft.carrier.CarrierLayout;
import ch.rere232.skofcraft.entity.AircraftCarrier;
import ch.rere232.skofcraft.registry.SkofcraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The carrier's helm, on the bridge facing the bow. Right-click it to take command: the carrier it belongs to
 * is worked out from the helm's position and facing, so a helm placed anywhere else does nothing.
 */
public class CarrierHelmBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 3.0D, 15.0D, 13.0D, 13.0D);
    private static final float MIN_INTEGRITY = 0.6F;

    public CarrierHelmBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** The keel origin of the carrier whose bridge holds a helm at {@code helmPos}. */
    public static BlockPos originOf(BlockPos helmPos, Direction facing) {
        return helmPos.subtract(CarrierLayout.HELM.rotate(CarrierBlocks.rotationFor(facing)));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer captain) || !(level instanceof ServerLevel serverLevel) || player.isPassenger()) {
            return InteractionResult.PASS;
        }
        Direction facing = state.getValue(FACING);
        BlockPos origin = originOf(pos, facing);
        if (CarrierBlocks.integrity(level, origin, facing) < MIN_INTEGRITY) {
            player.displayClientMessage(Component.translatable("message.skofcraft.carrier_not_a_ship"), true);
            return InteractionResult.CONSUME;
        }
        CarrierBlocks blocks = CarrierBlocks.capture(level, origin, facing);
        if (!blocks.contains(SkofcraftBlocks.NUCLEAR_REACTOR.get())) {
            player.displayClientMessage(Component.translatable("message.skofcraft.carrier_no_reactor"), true);
            return InteractionResult.CONSUME;
        }
        AircraftCarrier.getUnderway(serverLevel, origin, facing, blocks, captain);
        SkofcraftAdvancements.award(captain, SkofcraftAdvancements.ANCHORS_AWEIGH);
        return InteractionResult.CONSUME;
    }
}
