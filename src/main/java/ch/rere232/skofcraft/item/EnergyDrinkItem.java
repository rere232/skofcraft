package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * A can of energy drink: a short burst of effects, gives the empty can back. The caffeine counts toward the same
 * jitters as SKOFee, so mixing cans and cups is no way around it.
 */
public class EnergyDrinkItem extends Item {
    public enum Kind {
        // Speed and Haste II for 3 minutes
        WHITE_MONSTER(() -> List.of(
                new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 180, 0),
                new MobEffectInstance(MobEffects.DIG_SPEED, 20 * 180, 1))),
        // Gives you wings: Jump Boost II and Slow Falling for a minute, Speed for two
        RED_BULL(() -> List.of(
                new MobEffectInstance(MobEffects.JUMP, 20 * 60, 1),
                new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 60, 0),
                new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 120, 0)));

        private final Supplier<List<MobEffectInstance>> effects;

        Kind(Supplier<List<MobEffectInstance>> effects) {
            this.effects = effects;
        }
    }

    private final Kind kind;

    public EnergyDrinkItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
            serverPlayer.awardStat(Stats.ITEM_USED.get(this));
        }

        if (!level.isClientSide) {
            for (MobEffectInstance effect : kind.effects.get()) {
                entity.addEffect(effect);
            }
            if (entity instanceof Player player) {
                SkofeeItem.applyStreak(player, level, "message.skofcraft.energy_drink_jitters");
            }
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.6F, 1.3F);
        }

        if (entity instanceof Player player && player.getAbilities().instabuild) {
            return stack;
        }
        stack.shrink(1);
        ItemStack can = new ItemStack(SkofcraftItems.EMPTY_CAN.get());
        if (stack.isEmpty()) {
            return can;
        }
        if (entity instanceof Player player && !player.getInventory().add(can)) {
            player.drop(can, false);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 24;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".tooltip"));
    }
}
