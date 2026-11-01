package ch.rere232.skofcraft.block;

import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.blockentity.BigRedButtonBlockEntity;
import ch.rere232.skofcraft.network.ButtonCrashS2CPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.network.PacketDistributor;

/** Do NOT press this button. Pressing it crashes the game of whoever pressed it (and only theirs). */
public class BigRedButtonBlock extends BaseEntityBlock {
    public static final BooleanProperty PRESSED = BlockStateProperties.POWERED;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1.0D, 0.0D, 1.0D, 15.0D, 4.0D, 15.0D), Block.box(4.0D, 4.0D, 4.0D, 12.0D, 9.0D, 12.0D));
    private static final VoxelShape SHAPE_PRESSED = Shapes.or(Block.box(1.0D, 0.0D, 1.0D, 15.0D, 4.0D, 15.0D), Block.box(4.0D, 4.0D, 4.0D, 12.0D, 5.0D, 12.0D));
    private static final int PRESS_TICKS = 20;

    public BigRedButtonBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PRESSED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PRESSED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PRESSED) ? SHAPE_PRESSED : SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BigRedButtonBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(PRESSED)) {
            return InteractionResult.CONSUME;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(PRESSED, true), Block.UPDATE_ALL);
            level.scheduleTick(pos, this, PRESS_TICKS);
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 1.0F, 0.6F);
            if (player instanceof ServerPlayer serverPlayer && !(player instanceof FakePlayer)) {
                // Award first, so it's saved along with the rest when the game goes down
                SkofcraftAdvancements.award(serverPlayer, SkofcraftAdvancements.TOLD_YOU_SO);
                SkofcraftNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new ButtonCrashS2CPacket());
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PRESSED)) {
            level.setBlock(pos, state.setValue(PRESSED, false), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, 1.0F, 0.5F);
        }
    }
}
