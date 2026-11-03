package ch.rere232.skofcraft.block;

import ch.rere232.skofcraft.blockentity.ChichaBlockEntity;
import ch.rere232.skofcraft.blockentity.SkofcraftBlockEntities;
import ch.rere232.skofcraft.item.ChichaPipeItem;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Hookah with four hoses that players can take and smoke from within 4 blocks. */
public class ChichaBlock extends BaseEntityBlock {
    /** True while the hose is hanging on the chicha; false while a player holds it. */
    public static final BooleanProperty[] TUBES = {
            BooleanProperty.create("tube_0"),
            BooleanProperty.create("tube_1"),
            BooleanProperty.create("tube_2"),
            BooleanProperty.create("tube_3")
    };

    private static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D);
    private static final int PUFFS_PER_TOBACCO = 8;

    public ChichaBlock(Properties properties) {
        super(properties);
        BlockState state = this.stateDefinition.any();
        for (BooleanProperty tube : TUBES) {
            state = state.setValue(tube, true);
        }
        this.registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TUBES);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChichaBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, SkofcraftBlockEntities.CHICHA.get(), (l, p, s, be) -> be.tick());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        boolean holdingPipe = held.getItem() instanceof ChichaPipeItem;
        if (holdingPipe && !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof ChichaBlockEntity chicha)) {
            return InteractionResult.PASS;
        }

        if (!holdingPipe && held.is(SkofcraftItems.TOBACCO_DUST.get())) {
            int added = chicha.addPuffs(PUFFS_PER_TOBACCO);
            if (added <= 0) {
                player.displayClientMessage(Component.translatable("message.skofcraft.chicha_full"), true);
            } else {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                player.displayClientMessage(Component.translatable("message.skofcraft.chicha_filled", chicha.getPuffs(), ChichaBlockEntity.MAX_PUFFS), true);
            }
            return InteractionResult.CONSUME;
        }

        String flavorId = flavorIdOf(held);
        if (!holdingPipe && flavorId != null) {
            chicha.setFlavor(flavorId);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            player.displayClientMessage(Component.translatable("message.skofcraft.chicha_flavor", flavorName(flavorId)), true);
            return InteractionResult.CONSUME;
        }

        if (!holdingPipe && player.isShiftKeyDown()) {
            String flavor = chicha.getFlavor().isEmpty() ? Component.translatable("message.skofcraft.chicha_no_flavor").getString() : flavorName(chicha.getFlavor());
            player.displayClientMessage(Component.translatable("message.skofcraft.chicha_status", chicha.getPuffs(), ChichaBlockEntity.MAX_PUFFS, flavor), true);
            return InteractionResult.CONSUME;
        }

        int mine = chicha.tubeOf(player.getUUID());
        if (mine >= 0) {
            returnTube(player, chicha, mine);
            player.displayClientMessage(Component.translatable("message.skofcraft.chicha_returned"), true);
            return InteractionResult.CONSUME;
        }

        int free = chicha.freeTube();
        if (free < 0) {
            player.displayClientMessage(Component.translatable("message.skofcraft.chicha_tubes_taken"), true);
            return InteractionResult.CONSUME;
        }
        ItemStack pipe = ChichaPipeItem.create(level, pos, free);
        if (!player.addItem(pipe)) {
            player.displayClientMessage(Component.translatable("message.skofcraft.chicha_inventory_full"), true);
            return InteractionResult.CONSUME;
        }
        chicha.assign(free, player.getUUID());
        player.displayClientMessage(Component.translatable("message.skofcraft.chicha_taken"), true);
        return InteractionResult.CONSUME;
    }

    private static void returnTube(Player player, ChichaBlockEntity chicha, int tube) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (ChichaPipeItem.matches(inventory.getItem(slot), chicha.getBlockPos(), tube)) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
        chicha.release(tube);
    }

    @Nullable
    private static String flavorIdOf(ItemStack stack) {
        Item item = stack.getItem();
        boolean flavor = item == SkofcraftItems.FLAVOR_MINT.get()
                || item == SkofcraftItems.FLAVOR_ICE_MINT.get()
                || item == SkofcraftItems.FLAVOR_BERRY.get()
                || item == SkofcraftItems.FLAVOR_CITRUS.get()
                || item == SkofcraftItems.FLAVOR_LICORICE.get()
                || item == SkofcraftItems.FLAVOR_COLA.get()
                || item == SkofcraftItems.FLAVOR_COFFEE.get()
                || item == SkofcraftItems.FLAVOR_NATURE.get()
                || item == SkofcraftItems.FLAVOR_REGENTS_BLEND.get()
                || item == SkofcraftItems.FLAVOR_ROYAL_RESERVE.get();
        if (stack.isEmpty() || !flavor) {
            return null;
        }
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static String flavorName(String flavorId) {
        return flavorId.replace("skofcraft:flavor_", "").replace('_', ' ');
    }
}
