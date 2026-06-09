package ch.rere232.skofcraft.block;

import ch.rere232.skofcraft.blockentity.FEDryerBlockEntity;
import ch.rere232.skofcraft.blockentity.SkofcraftBlockEntities;
import ch.rere232.skofcraft.menu.FEDryerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class FEDryerBlock extends BaseEntityBlock {
    private final boolean requiresEnergy;

    public FEDryerBlock(Properties properties) {
        this(properties, true);
    }

    public FEDryerBlock(Properties properties, boolean requiresEnergy) {
        super(properties);
        this.requiresEnergy = requiresEnergy;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new FEDryerBlockEntity(blockPos, blockState, requiresEnergy);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, SkofcraftBlockEntities.FE_DRYER.get(), (l, p, s, be) -> be.tick());
    }

    @Override
    public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult result) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof FEDryerBlockEntity dryer) {
                NetworkHooks.openScreen(serverPlayer, new net.minecraft.world.SimpleMenuProvider(
                    (windowId, playerInventory, p) -> new FEDryerMenu(windowId, playerInventory, dryer),
                    Component.literal("FE Dryer")
                ));
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }
}
