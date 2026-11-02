package ch.rere232.skofcraft.block;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.api.IcbmSpecialTarget;
import ch.rere232.skofcraft.blockentity.LaunchPadBlockEntity;
import ch.rere232.skofcraft.item.OrbitalDesignatorItem;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import ch.rere232.skofcraft.space.OrbitData;
import ch.rere232.skofcraft.space.SatelliteType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Satellite launcher. Right-click with a satellite to load it, with Jet Fuel Canisters to fuel the rocket, then
 * right-click with an empty hand (or send a redstone pulse) to start the countdown. Sneak-right-click with an
 * empty hand takes the satellite back out and reports what is in orbit.
 */
public class LaunchPadBlock extends BaseEntityBlock {
    /** Height of the pad's deck, where the rocket stands. */
    public static final double TOP = 4.0D / 16.0D;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D);

    public LaunchPadBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LaunchPadBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof LaunchPadBlockEntity pad)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        boolean creative = player.getAbilities().instabuild;

        if (LaunchPadBlockEntity.isPayload(held)) {
            if (pad.hasSatellite()) {
                message(player, Component.translatable("message.skofcraft.launch_pad_occupied"));
                return InteractionResult.CONSUME;
            }
            pad.loadSatellite(held, player.getUUID());
            if (pad.hasIcbm()) {
                pad.programFrom(player);
                level.playSound(null, pos, SkofcraftSounds.ICBM_ARM.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            }
            if (!creative) {
                held.shrink(1);
            }
            level.playSound(null, pos, SkofcraftSounds.PAYLOAD_LOAD.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            status(player, pad);
            return InteractionResult.CONSUME;
        }

        if (held.getItem() instanceof OrbitalDesignatorItem && pad.hasIcbm()) {
            // Reprogram the missile with the player's current mark
            pad.programFrom(player);
            level.playSound(null, pos, SkofcraftSounds.UI_CLICK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            status(player, pad);
            return InteractionResult.CONSUME;
        }

        if (held.is(SkofcraftItems.JET_FUEL_CANISTER.get())) {
            if (!pad.addFuel()) {
                message(player, Component.translatable("message.skofcraft.launch_pad_fuel_full"));
                return InteractionResult.CONSUME;
            }
            if (!creative) {
                held.shrink(1);
                ItemStack empty = new ItemStack(SkofcraftItems.EMPTY_FUEL_CANISTER.get());
                if (held.isEmpty()) {
                    player.setItemInHand(hand, empty);
                } else if (!player.getInventory().add(empty)) {
                    player.drop(empty, false);
                }
            }
            level.playSound(null, pos, SkofcraftSounds.FUEL_PUMP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            status(player, pad);
            return InteractionResult.CONSUME;
        }

        if (!held.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            if (pad.hasSatellite()) {
                ItemStack taken = pad.unload();
                if (!player.getInventory().add(taken)) {
                    player.drop(taken, false);
                }
            }
            OrbitData orbit = OrbitData.get((ServerLevel) level);
            message(player, Component.translatable("message.skofcraft.orbit_status",
                    orbit.count(SatelliteType.RECON), orbit.count(SatelliteType.STRIKE)).withStyle(ChatFormatting.AQUA));
            return InteractionResult.CONSUME;
        }
        if (pad.launch((ServerLevel) level, player.getUUID())) {
            level.playSound(null, pos, SkofcraftSounds.LAUNCH_ARM.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
        } else {
            status(player, pad);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) {
            return;
        }
        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED)) {
            return;
        }
        level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        if (powered && level.getBlockEntity(pos) instanceof LaunchPadBlockEntity pad && pad.isReady()) {
            // A redstone launch is credited to whoever loaded the satellite; the fallback is never used then
            pad.launch((ServerLevel) level, new java.util.UUID(0L, 0L));
            level.playSound(null, pos, SkofcraftSounds.LAUNCH_ARM.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
        }
    }

    private static void status(Player player, LaunchPadBlockEntity pad) {
        Component payload = pad.hasSatellite() ? pad.getSatellite().getHoverName() : Component.translatable("message.skofcraft.launch_pad_empty");
        if (pad.hasIcbm()) {
            BlockPos target = pad.getTarget();
            IcbmSpecialTarget special = pad.getSpecialTarget();
            payload = special != null
                    ? special.padStatus(payload)
                    : target == null
                    ? Component.translatable("message.skofcraft.icbm_no_target", payload).withStyle(ChatFormatting.RED)
                    : Component.translatable("message.skofcraft.icbm_target", payload, target.getX(), target.getZ()).withStyle(ChatFormatting.LIGHT_PURPLE);
        }
        message(player, Component.translatable(pad.isReady() ? "message.skofcraft.launch_pad_ready" : "message.skofcraft.launch_pad_status",
                payload, pad.getFuel(), pad.fuelNeeded()));
    }

    private static void message(Player player, Component text) {
        player.displayClientMessage(text, true);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock() && level.getBlockEntity(pos) instanceof LaunchPadBlockEntity pad) {
            if (pad.hasSatellite()) {
                popResource(level, pos, pad.unload());
            }
            if (pad.getFuel() > 0) {
                popResource(level, pos, new ItemStack(SkofcraftItems.JET_FUEL_CANISTER.get(), pad.getFuel()));
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
