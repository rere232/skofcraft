package ch.rere232.skofcraft.block;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The sword in the stone. There is no way to get it out and no code that would let you: right-clicking only ever gets
 * you a message. Unbreakable outside creative, which also means a Giga ICBM goes around it.
 */
public class ExcaliburBlock extends Block {
    private static final int REFUSALS = 6;
    private static final VoxelShape STONE = Shapes.or(box(1.0D, 0.0D, 1.0D, 15.0D, 7.0D, 15.0D), box(3.0D, 7.0D, 3.0D, 13.0D, 9.0D, 13.0D));
    private static final VoxelShape WITH_SWORD = Shapes.or(STONE, box(5.0D, 9.0D, 7.0D, 11.0D, 25.0D, 9.0D));

    public ExcaliburBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.skofcraft.excalibur." + level.random.nextInt(REFUSALS))
                    .withStyle(ChatFormatting.GOLD), true);
            level.playSound(null, pos, SkofcraftSounds.SWORD_CLANG.get(), SoundSource.BLOCKS, 0.3F, 1.0F);
            SkofcraftAdvancements.award(player, SkofcraftAdvancements.NOT_WORTHY);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return WITH_SWORD;
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
