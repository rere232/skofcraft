package ch.rere232.skofcraft.block;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.blockentity.DefenseTurretBlockEntity;
import ch.rere232.skofcraft.blockentity.SkofcraftBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Automatic ship defence: a pedestal with a turret head drawn by the block entity renderer. Right-click to switch
 * between engaging hostile mobs only and engaging every threat (also players and manned aircraft that are not the
 * owner's or their team's).
 */
public class DefenseTurretBlock extends BaseEntityBlock {
    /** What the turret fires. */
    public enum Weapon {
        /** Gatling cannon: fast bullets at anything it may engage. */
        CIWS(40.0D, 2, 4.0F, 1.5F),
        /** Missile launcher: homing missiles at aircraft and flying mobs only. */
        SAM(96.0D, 30, 2.4F, 0.0F);

        public final double range;
        public final int cooldown;
        public final float speed;
        public final float inaccuracy;

        Weapon(double range, int cooldown, float speed, float inaccuracy) {
            this.range = range;
            this.cooldown = cooldown;
            this.speed = speed;
            this.inaccuracy = inaccuracy;
        }
    }

    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 10.0D, 15.0D);

    public final Weapon weapon;

    public DefenseTurretBlock(Properties properties, Weapon weapon) {
        super(properties);
        this.weapon = weapon;
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
        return new DefenseTurretBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, SkofcraftBlockEntities.DEFENSE_TURRET.get(), level.isClientSide
                ? (l, p, s, turret) -> turret.clientTick()
                : (l, p, s, turret) -> turret.serverTick());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof DefenseTurretBlockEntity turret) {
            turret.setOwner(player.getUUID());
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof DefenseTurretBlockEntity turret) {
            if (turret.getOwner() == null) {
                // Unowned turrets (the ones a carrier ships with) belong to whoever first sets them
                turret.setOwner(player.getUUID());
            }
            boolean allThreats = !turret.engagesAllThreats();
            turret.setEngageAllThreats(allThreats);
            level.playSound(null, pos, SkofcraftSounds.SWITCH_CLICK.get(), SoundSource.BLOCKS, 0.6F, allThreats ? 0.6F : 0.9F);
            player.displayClientMessage(Component.translatable("message.skofcraft.turret_mode",
                    Component.translatable(getDescriptionId()),
                    Component.translatable(allThreats ? "message.skofcraft.turret_mode.all" : "message.skofcraft.turret_mode.hostiles")
                            .withStyle(allThreats ? ChatFormatting.RED : ChatFormatting.GREEN)), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
