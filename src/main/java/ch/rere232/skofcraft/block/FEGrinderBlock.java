package ch.rere232.skofcraft.block;

import ch.rere232.skofcraft.blockentity.FEGrinderBlockEntity;
import ch.rere232.skofcraft.blockentity.SkofcraftBlockEntities;
import ch.rere232.skofcraft.menu.FEGrinderMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class FEGrinderBlock extends BaseEntityBlock {
    private final boolean requiresEnergy;

    public FEGrinderBlock(Properties properties) {
        this(properties, true);
    }

    public FEGrinderBlock(Properties properties, boolean requiresEnergy) {
        super(properties);
        this.requiresEnergy = requiresEnergy;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new FEGrinderBlockEntity(blockPos, blockState, requiresEnergy);
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
        if (player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof FEGrinderBlockEntity grinder) {
                if (!requiresEnergy && player.isShiftKeyDown()) {
                    if (grinder.manualCrank()) {
                        return InteractionResult.CONSUME;
                    }
                }
                NetworkHooks.openScreen(serverPlayer, new net.minecraft.world.SimpleMenuProvider(
                    (windowId, playerInventory, p) -> new FEGrinderMenu(windowId, playerInventory, grinder),
                    Component.literal(requiresEnergy ? "FE Grinder" : "Manual Grinder")
                ), buf -> buf.writeBlockPos(blockPos));
            }
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
