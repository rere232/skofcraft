package ch.rere232.skofcraft.block;

import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.hr.MandatoryHr;
import ch.rere232.skofcraft.kingdom.PastelParadise;
import ch.rere232.skofcraft.registry.SkofcraftPastel;
import ch.rere232.skofcraft.registry.SkofcraftSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Talk it out. In Pastel Paradise, Heart Tokens take incidents off your HR record, one at a time. */
public class TherapyCouchBlock extends Block {
    public static final int SESSION_PRICE = 8;
    private static final int SESSION_COOLDOWN = 60;
    private static final VoxelShape SHAPE = box(0.0D, 0.0D, 2.0D, 16.0D, 12.0D, 14.0D);

    public TherapyCouchBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            session(level, pos, serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void session(Level level, BlockPos pos, ServerPlayer player) {
        if (player.getCooldowns().isOnCooldown(asItem())) {
            return;
        }
        if (!PastelParadise.isIn(player)) {
            player.displayClientMessage(Component.translatable("message.skofcraft.therapy_elsewhere"), true);
            return;
        }
        int incidents = MandatoryHr.incidents(player);
        if (incidents <= 0) {
            player.displayClientMessage(Component.translatable("message.skofcraft.therapy_clean"), true);
            return;
        }
        if (!takeHearts(player)) {
            player.displayClientMessage(Component.translatable("message.skofcraft.therapy_cost", SESSION_PRICE), true);
            return;
        }
        player.getCooldowns().addCooldown(asItem(), SESSION_COOLDOWN);
        MandatoryHr.forgiveOne(player);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
        level.playSound(null, pos, SkofcraftSounds.PLUSH_SOFT.get(), SoundSource.BLOCKS, 1.0F, 0.9F);
        player.displayClientMessage(Component.translatable("message.skofcraft.therapy_done", incidents - 1).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        SkofcraftAdvancements.award(player, SkofcraftAdvancements.TALKED_IT_OUT);
    }

    private static boolean takeHearts(Player player) {
        ItemStack token = new ItemStack(SkofcraftPastel.HEART_TOKEN.get());
        var inventory = player.getInventory();
        if (inventory.countItem(token.getItem()) < SESSION_PRICE) {
            return false;
        }
        inventory.clearOrCountMatchingItems(stack -> stack.is(token.getItem()), SESSION_PRICE, player.inventoryMenu.getCraftSlots());
        return true;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.skofcraft.therapy_couch", SESSION_PRICE).withStyle(ChatFormatting.GRAY));
    }
}
