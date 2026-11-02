package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
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
import java.util.Locale;

/**
 * One item, one million beers, drunk one at a time. At 1.2 seconds each that's nearly fourteen days without stopping.
 * Each one adds to the nausea, and a player who really commits loses their sight for a while.
 */
public class OneMillionBeerItem extends Item {
    private static final int FULL = 1_000_000;
    private static final String TAG = "Beers";
    private static final int DRINK_TICKS = 24;
    private static final int NAUSEA_PER_BEER = 20 * 10;
    private static final int MAX_NAUSEA = 20 * 120;
    /** Past this much nausea, the next beer also takes your sight for a moment. */
    private static final int BLACKOUT_NAUSEA = 20 * 60;
    private static final int BLACKOUT_TICKS = 20 * 5;
    private static final int COURAGE_TICKS = 20 * 60;
    private static final int BAR_COLOR = 0xE8A317;

    public OneMillionBeerItem(Properties properties) {
        super(properties);
    }

    /** A stack without a count is a full one, which is what crafting and the creative tab give you. */
    private static int beersLeft(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().contains(TAG) ? stack.getTag().getInt(TAG) : FULL;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide) {
            return stack;
        }
        if (entity instanceof ServerPlayer player) {
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        applyBeer(entity);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SkofcraftSounds.BEER_GULP.get(), SoundSource.PLAYERS, 0.6F, 1.0F);

        if (entity instanceof Player player && player.getAbilities().instabuild) {
            return stack;
        }
        int left = beersLeft(stack) - 1;
        if (left > 0) {
            stack.getOrCreateTag().putInt(TAG, left);
            return stack;
        }
        SkofcraftAdvancements.award(entity, SkofcraftAdvancements.LAST_CALL);
        return new ItemStack(SkofcraftItems.EMPTY_CAN.get());
    }

    private static void applyBeer(LivingEntity entity) {
        MobEffectInstance current = entity.getEffect(MobEffects.CONFUSION);
        int nausea = (current == null ? 0 : current.getDuration()) + NAUSEA_PER_BEER;
        entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, Math.min(nausea, MAX_NAUSEA), 0));
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, COURAGE_TICKS, 0));
        if (nausea > BLACKOUT_NAUSEA) {
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLACKOUT_TICKS, 0));
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return DRINK_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return beersLeft(stack) < FULL;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        // Never rounds down to nothing while there's still a beer in it
        return Mth.clamp(Math.round(13.0F * beersLeft(stack) / FULL), 1, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.skofcraft.one_million_beer", String.format(Locale.ROOT, "%,d", beersLeft(stack)))
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.skofcraft.one_million_beer.warning").withStyle(ChatFormatting.GRAY));
    }
}
