package ch.rere232.skofcraft.block;

import ch.rere232.skofcraft.blockentity.SkofeeMachineBlockEntity;
import ch.rere232.skofcraft.client.SkofeeClient;
import ch.rere232.skofcraft.item.SkofeeItem;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

/**
 * Espresso machine. Fill it with Water Buckets and Ground SKOFee, then right-click it
 * with an Empty SKOFee Cup to play the brewing minigame and pour a Cup of SKOFee.
 */
public class SkofeeMachineBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 1.0D, 14.0D, 14.0D, 15.0D);

    public SkofeeMachineBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
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
        return new SkofeeMachineBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof SkofeeMachineBlockEntity machine)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide) {
            if (held.is(SkofcraftItems.SKOFEE_CUP.get()) && machine.canBrew()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SkofeeClient.openBrew(pos));
            }
            return InteractionResult.SUCCESS;
        }
        boolean creative = player.getAbilities().instabuild;

        if (held.is(Items.WATER_BUCKET)) {
            if (!machine.addBucket()) {
                player.displayClientMessage(Component.translatable("message.skofcraft.skofee_water_full"), true);
                return InteractionResult.CONSUME;
            }
            if (!creative) {
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            }
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            status(player, machine);
            return InteractionResult.CONSUME;
        }

        if (held.is(SkofcraftItems.SKOFEE_GROUND.get())) {
            int added = machine.addDoses(player.isShiftKeyDown() ? held.getCount() : 1);
            if (added <= 0) {
                player.displayClientMessage(Component.translatable("message.skofcraft.skofee_grounds_full"), true);
                return InteractionResult.CONSUME;
            }
            if (!creative) {
                held.shrink(added);
            }
            level.playSound(null, pos, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 0.6F, 1.4F);
            status(player, machine);
            return InteractionResult.CONSUME;
        }

        if (held.is(SkofcraftItems.SKOFEE_CUP.get())) {
            // The client opens the brewing minigame; the cup is poured once SkofeeBrewC2SPacket comes back.
            if (!machine.canBrew()) {
                player.displayClientMessage(Component.translatable(machine.getWater() <= 0
                        ? "message.skofcraft.skofee_no_water" : "message.skofcraft.skofee_no_grounds"), true);
            }
            return InteractionResult.CONSUME;
        }

        if (held.isEmpty()) {
            status(player, machine);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    /** Pours a cup after the minigame: uses one water and one dose and swaps an Empty Cup in the main hand for a Cup of SKOFee. */
    public static void pour(ServerPlayer player, BlockPos pos, SkofeeMachineBlockEntity machine, int score) {
        ItemStack held = player.getMainHandItem();
        if (!held.is(SkofcraftItems.SKOFEE_CUP.get())) {
            return;
        }
        if (!machine.canBrew()) {
            player.displayClientMessage(Component.translatable(machine.getWater() <= 0
                    ? "message.skofcraft.skofee_no_water" : "message.skofcraft.skofee_no_grounds"), true);
            return;
        }
        machine.brew();
        int quality = SkofeeItem.qualityFromScore(score);
        ItemStack drink = SkofeeItem.withQuality(new ItemStack(SkofcraftItems.SKOFEE.get()), quality);
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        if (held.isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, drink);
        } else if (!player.getInventory().add(drink)) {
            player.drop(drink, false);
        }

        ServerLevel level = player.serverLevel();
        if (quality == SkofeeItem.QUALITY_BURNT) {
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.2F);
            level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5D, pos.getY() + 0.8D, pos.getZ() + 0.5D, 10, 0.15D, 0.1D, 0.15D, 0.01D);
        } else {
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8F, 1.3F);
            level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5D, pos.getY() + 0.8D, pos.getZ() + 0.5D, 6, 0.15D, 0.1D, 0.15D, 0.01D);
        }
        if (quality == SkofeeItem.QUALITY_PERFECT) {
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.5F, 1.6F);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 8, 0.3D, 0.2D, 0.3D, 0.0D);
        }
        player.displayClientMessage(Component.translatable("message.skofcraft.skofee_brewed", SkofeeItem.qualityName(quality)), true);
    }

    private static void status(Player player, SkofeeMachineBlockEntity machine) {
        player.displayClientMessage(Component.translatable("message.skofcraft.skofee_status",
                machine.getWater(), SkofeeMachineBlockEntity.MAX_WATER,
                machine.getDoses(), SkofeeMachineBlockEntity.MAX_DOSES), true);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) != 0 || !(level.getBlockEntity(pos) instanceof SkofeeMachineBlockEntity machine) || machine.getWater() <= 0) {
            return;
        }
        level.addParticle(ParticleTypes.WHITE_ASH, pos.getX() + 0.3D + random.nextDouble() * 0.4D, pos.getY() + 0.95D,
                pos.getZ() + 0.3D + random.nextDouble() * 0.4D, 0.0D, 0.03D, 0.0D);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock() && level.getBlockEntity(pos) instanceof SkofeeMachineBlockEntity machine) {
            int doses = machine.getDoses();
            while (doses > 0) {
                int count = Math.min(64, doses);
                popResource(level, pos, new ItemStack(SkofcraftItems.SKOFEE_GROUND.get(), count));
                doses -= count;
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
