package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.gums.GumSlotData;
import ch.rere232.skofcraft.kingdom.SnusKingdom;
import ch.rere232.skofcraft.phone.PhoneAddiction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/** Addictol: wipes tolerance and addiction back to zero. */
public class AddictolItem extends Item {
    public AddictolItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player) {
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
            }
            GumSlotData.resetDependence(player);
            PhoneAddiction.reset(player);
            player.removeEffect(MobEffects.WEAKNESS);
            player.removeEffect(MobEffects.DIG_SLOWDOWN);
            player.removeEffect(MobEffects.CONFUSION);
            if (player instanceof ServerPlayer serverPlayer && SnusKingdom.isIn(serverPlayer)) {
                SnusKingdom.wakeUp(serverPlayer, "addictol");
            }
            player.displayClientMessage(Component.translatable("message.skofcraft.addictol_used"), true);

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SkofcraftSounds.PILL_CHIME.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0D, player.getZ(), 14, 0.4D, 0.5D, 0.4D, 0.0D);
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return stack;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.skofcraft.addictol"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
