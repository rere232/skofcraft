package ch.rere232.skofcraft.block;

import ch.rere232.skofcraft.kingdom.SnusKingdom;
import ch.rere232.skofcraft.registry.SkofcraftPastel;
import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The sword in the stone. Right-clicking only ever gets you a message, unless you are in the Snus Kingdom wearing all of
 * The Rightful Fit: then it comes out, the empty stone stays, and the sword can be put back by right-clicking the stone
 * with it. Unbreakable outside creative, which also means a Giga ICBM goes around it.
 */
public class ExcaliburBlock extends Block {
    private static final int REFUSALS = 6;
    private static final VoxelShape STONE = Shapes.or(box(1.0D, 0.0D, 1.0D, 15.0D, 7.0D, 15.0D), box(3.0D, 7.0D, 3.0D, 13.0D, 9.0D, 13.0D));
    private static final VoxelShape WITH_SWORD = Shapes.or(STONE, box(5.0D, 9.0D, 7.0D, 11.0D, 25.0D, 9.0D));
    public static final BooleanProperty PULLED = BooleanProperty.create("pulled");

    public ExcaliburBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PULLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PULLED);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            if (state.getValue(PULLED)) {
                useEmptyStone(state, level, pos, player);
                return InteractionResult.SUCCESS;
            }
            if (SkofcraftPastel.isWearingRegalia(player)) {
                if (player instanceof ServerPlayer serverPlayer && SnusKingdom.isIn(serverPlayer)) {
                    level.setBlockAndUpdate(pos, state.setValue(PULLED, true));
                    ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(SkofcraftPastel.EXCALIBUR_SWORD.get()));
                    level.playSound(null, pos, SkofcraftSounds.SWORD_CLANG.get(), SoundSource.BLOCKS, 1.0F, 0.8F);
                    level.playSound(null, pos, SkofcraftSounds.JACKPOT.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                    SnusKingdom.crown(serverPlayer, pos);
                } else {
                    player.displayClientMessage(Component.translatable("message.skofcraft.excalibur_wrong_stone").withStyle(ChatFormatting.GOLD), true);
                }
                return InteractionResult.SUCCESS;
            }
            player.displayClientMessage(Component.translatable("message.skofcraft.excalibur." + level.random.nextInt(REFUSALS))
                    .withStyle(ChatFormatting.GOLD), true);
            level.playSound(null, pos, SkofcraftSounds.SWORD_CLANG.get(), SoundSource.BLOCKS, 0.3F, 1.0F);
            SkofcraftAdvancements.award(player, SkofcraftAdvancements.NOT_WORTHY);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Holding the sword, the stone takes it back for whoever is next. Otherwise it's just a rock with a slot in it. */
    private static void useEmptyStone(BlockState state, Level level, BlockPos pos, Player player) {
        ItemStack held = player.getMainHandItem();
        if (held.is(SkofcraftPastel.EXCALIBUR_SWORD.get())) {
            held.shrink(1);
            level.setBlockAndUpdate(pos, state.setValue(PULLED, false));
            level.playSound(null, pos, SkofcraftSounds.SWORD_CLANG.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("message.skofcraft.excalibur_returned").withStyle(ChatFormatting.GOLD), true);
        } else {
            player.displayClientMessage(Component.translatable("message.skofcraft.excalibur_empty").withStyle(ChatFormatting.GOLD), true);
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PULLED) ? STONE : WITH_SWORD;
    }

    /** You can stand on the stone, and walk into the sword no further than the stone lets you. */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return STONE;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.skofcraft.excalibur").withStyle(ChatFormatting.GRAY));
    }
}
