package ch.rere232.skofcraft.block;

import ch.rere232.skofcraft.kingdom.PastelParadise;
import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.phone.PhoneAddiction;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The famous soft shark. Hugging it, placed or in hand, calms you down. */
public class BlahajBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape SHAPE_NS = Block.box(1.0D, 0.0D, 0.0D, 15.0D, 11.0D, 16.0D);
    private static final VoxelShape SHAPE_EW = Block.box(0.0D, 0.0D, 1.0D, 16.0D, 11.0D, 15.0D);
    private static final int HUG_COOLDOWN_TICKS = 60;
    private static final int HUG_ADDICTION_RELIEF = 5;
    private static final int HUG_MESSAGES = 4;

    public BlahajBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (!level.isClientSide) {
            hug(player, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Server only: a hug heals a little and eases phone addiction, at most once every 3 seconds. */
    public static void hug(Player player, double x, double y, double z) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (player.getCooldowns().isOnCooldown(SkofcraftItems.BLAHAJ.get())) {
            return;
        }
        player.getCooldowns().addCooldown(SkofcraftItems.BLAHAJ.get(), HUG_COOLDOWN_TICKS);

        PhoneAddiction.reduce(serverPlayer, HUG_ADDICTION_RELIEF);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, false, true));
        level.sendParticles(ParticleTypes.HEART, x, y + 0.4D, z, 6, 0.3D, 0.3D, 0.3D, 0.0D);
        level.playSound(null, x, y, z, SkofcraftSounds.BLAHAJ_SQUEAK.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
        level.playSound(null, x, y, z, SkofcraftSounds.PLUSH_SOFT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        serverPlayer.displayClientMessage(
                Component.translatable("message.skofcraft.blahaj_hug." + player.getRandom().nextInt(HUG_MESSAGES)), true);
        PastelParadise.onHug(serverPlayer);
    }
}
