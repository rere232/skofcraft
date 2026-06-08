package ch.rere232.skofcraft.block;

import ch.rere232.skofcraft.blockentity.FEGrinderBlockEntity;
import ch.rere232.skofcraft.blockentity.SkofcraftBlockEntities;
import ch.rere232.skofcraft.menu.FEGrinderMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class FEGrinderBlock extends BaseEntityBlock {
    public FEGrinderBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new FEGrinderBlockEntity(blockPos, blockState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, SkofcraftBlockEntities.FE_GRINDER.get(), (l, p, s, be) -> be.tick());
    }

    @Override
    public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult result) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(blockPos);
        if (be instanceof FEGrinderBlockEntity grinder) {
            player.openMenu(new net.minecraft.world.SimpleMenuProvider((windowId, playerInventory, p) -> 
                new FEGrinderMenu(windowId, playerInventory, grinder), 
                net.minecraft.network.chat.Component.literal("FE Grinder")));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState newBlockState, boolean isMoving) {
        if (blockState.getBlock() != newBlockState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(blockPos);
            if (blockEntity instanceof FEGrinderBlockEntity grinder) {
                for (int i = 0; i < grinder.getInputSlots().getContainerSize(); i++) {
                    popResource(level, blockPos, grinder.getInputSlots().getItem(i));
                }
                for (int i = 0; i < grinder.getOutputSlots().getContainerSize(); i++) {
                    popResource(level, blockPos, grinder.getOutputSlots().getItem(i));
                }
            }
        }
        super.onRemove(blockState, level, blockPos, newBlockState, isMoving);
    }
}
