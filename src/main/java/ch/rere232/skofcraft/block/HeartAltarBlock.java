package ch.rere232.skofcraft.block;

import ch.rere232.skofcraft.entity.TheAlgorithm;
import ch.rere232.skofcraft.kingdom.PastelParadise;
import ch.rere232.skofcraft.registry.SkofcraftEntities;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import ch.rere232.skofcraft.registry.SkofcraftSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Give it your phone and The Algorithm comes to collect the rest of you. Only works in Pastel Paradise. */
public class HeartAltarBlock extends Block {
    private static final double ONE_AT_A_TIME_RADIUS = 96.0D;

    public HeartAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(SkofcraftItems.PHONE.get())) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            summon(serverLevel, pos, serverPlayer, held);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void summon(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack phone) {
        if (!PastelParadise.isIn(player)) {
            player.displayClientMessage(Component.translatable("message.skofcraft.altar_elsewhere"), true);
            return;
        }
        // Peaceful would delete the boss on the next tick, and the phone with it
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            player.displayClientMessage(Component.translatable("message.skofcraft.altar_peaceful"), true);
            return;
        }
        if (!level.getEntitiesOfClass(TheAlgorithm.class, new AABB(pos).inflate(ONE_AT_A_TIME_RADIUS)).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.skofcraft.altar_busy"), true);
            return;
        }
        TheAlgorithm boss = SkofcraftEntities.THE_ALGORITHM.get().create(level);
        if (boss == null) {
            return;
        }
        phone.shrink(1);
        boss.moveTo(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, player.getYRot() + 180.0F, 0.0F);
        boss.setTarget(player);
        level.addFreshEntity(boss);
        level.playSound(null, pos, SkofcraftSounds.NUKE_WARNING.get(), SoundSource.HOSTILE, 0.6F, 1.6F);
        level.getPlayers(nearby -> nearby.blockPosition().closerThan(pos, 64.0D)).forEach(nearby ->
                nearby.displayClientMessage(Component.translatable("message.skofcraft.algorithm_arrives").withStyle(ChatFormatting.DARK_PURPLE), false));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.skofcraft.heart_altar").withStyle(ChatFormatting.GRAY));
    }
}
