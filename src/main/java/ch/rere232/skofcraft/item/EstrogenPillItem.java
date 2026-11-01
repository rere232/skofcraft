package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.registry.SkofcraftEffects;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
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

/** Estrogen Pill: 10 minutes of the Estrogen effect, plus a short burst of regeneration. */
public class EstrogenPillItem extends Item {
    private static final int EFFECT_TICKS = 20 * 60 * 10;

    public EstrogenPillItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 16;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            if (entity instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
            }
            entity.addEffect(new MobEffectInstance(SkofcraftEffects.ESTROGEN.get(), EFFECT_TICKS, 0));
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 8, 0));
            if (!(entity instanceof Player player && player.getAbilities().instabuild)) {
                stack.shrink(1);
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.skofcraft.estrogen_pill"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
