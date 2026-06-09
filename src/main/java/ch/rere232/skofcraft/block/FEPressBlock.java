package ch.rere232.skofcraft.block;

import ch.rere232.skofcraft.blockentity.FEPressBlockEntity;
import ch.rere232.skofcraft.blockentity.SkofcraftBlockEntities;
import ch.rere232.skofcraft.menu.FEPressMenu;
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

public class FEPressBlock extends BaseEntityBlock {
    private final boolean requiresEnergy;

    public FEPressBlock(Properties properties) {
        this(properties, true);
    }

    public FEPressBlock(Properties properties, boolean requiresEnergy) {
        super(properties);
        this.requiresEnergy = requiresEnergy;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new FEPressBlockEntity(blockPos, blockState, requiresEnergy);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, SkofcraftBlockEntities.FE_PRESS.get(), (l, p, s, be) -> be.tick());
    }

    @Override
    public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult result) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof FEPressBlockEntity press) {
                NetworkHooks.openScreen(serverPlayer, new net.minecraft.world.SimpleMenuProvider(
                    (windowId, playerInventory, p) -> new FEPressMenu(windowId, playerInventory, press),
                    Component.literal(requiresEnergy ? "FE Press" : "Manual Press")
                ));
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
            if (blockEntity instanceof FEPressBlockEntity press) {
                for (int i = 0; i < press.getInputSlots().getContainerSize(); i++) {
                    popResource(level, blockPos, press.getInputSlots().getItem(i));
                }
                for (int i = 0; i < press.getOutputSlots().getContainerSize(); i++) {
                    popResource(level, blockPos, press.getOutputSlots().getItem(i));
                }
            }
        }
        super.onRemove(blockState, level, blockPos, newBlockState, isMoving);
    }
}
