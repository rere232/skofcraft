package ch.rere232.skofcraft.block;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.registry.SkofcraftEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A string stretched across the floor. Walk into it and you go flat on your face: you're shoved forward, you drop what
 * you were holding, and you crawl for a few seconds. Unlike the vanilla tripwire, sneaking doesn't save you.
 */
public class EvilTripwireBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape SHAPE_X = box(0.0D, 1.0D, 6.0D, 16.0D, 2.5D, 10.0D);
    private static final VoxelShape SHAPE_Z = box(6.0D, 1.0D, 0.0D, 10.0D, 2.5D, 16.0D);
    /** The wire is at ankle height: anything higher than this steps over it. */
    private static final double ANKLE_HEIGHT = 0.25D;
    private static final int TRIPPED_TICKS = 60;
    /** Time to get up and walk away before the wire can catch you again. */
    private static final int GRACE_TICKS = 100;
    private static final double SHOVE = 0.7D;
    private static final float FALL_DAMAGE = 1.0F;
    private static final String LAST_TRIP = "SkofcraftLastTrip";

    public EvilTripwireBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    /** Runs across the way the player is facing, so it lies across the path they're looking down. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getClockWise().getAxis());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level instanceof ServerLevel serverLevel && entity instanceof LivingEntity living && canTrip(living, pos, serverLevel)) {
            trip(living, serverLevel);
        }
    }

    private static boolean canTrip(LivingEntity entity, BlockPos pos, ServerLevel level) {
        if (entity.isPassenger() || entity.isSpectator() || entity.getY() > pos.getY() + ANKLE_HEIGHT
                || (entity instanceof Player player && player.getAbilities().flying)) {
            return false;
        }
        CompoundTag data = entity.getPersistentData();
        return !data.contains(LAST_TRIP) || Math.abs(level.getGameTime() - data.getLong(LAST_TRIP)) >= GRACE_TICKS;
    }

    private static void trip(LivingEntity entity, ServerLevel level) {
        entity.getPersistentData().putLong(LAST_TRIP, level.getGameTime());
        entity.addEffect(new MobEffectInstance(SkofcraftEffects.TRIPPED.get(), TRIPPED_TICKS, 0));

        Vec3 look = entity.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        Vec3 shove = flat.lengthSqr() > 1.0E-4D ? flat.normalize().scale(SHOVE) : Vec3.ZERO;
        entity.setDeltaMovement(shove.x, 0.1D, shove.z);
        // A player's own client decides where they are, so it has to be told about the shove
        entity.hurtMarked = true;
        entity.hurt(level.damageSources().fall(), FALL_DAMAGE);

        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SkofcraftSounds.TRIP_THUD.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SkofcraftSounds.EVIL_CACKLE.get(), SoundSource.BLOCKS, 0.8F, 1.0F);

        if (entity instanceof Player player) {
            dropHeldItem(player);
            player.displayClientMessage(Component.translatable("message.skofcraft.evil_tripwire"), true);
            SkofcraftAdvancements.award(player, SkofcraftAdvancements.HAVE_A_NICE_TRIP);
        }
    }

    private static void dropHeldItem(Player player) {
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty() && !player.getAbilities().instabuild) {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.drop(held, false, true);
        }
    }
}
