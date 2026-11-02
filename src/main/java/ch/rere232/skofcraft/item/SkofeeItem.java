package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.config.SkofcraftConfig;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

/**
 * Cup of SKOFee: Speed and Haste, gives the empty cup back. Too many cups in a row give the jitters.
 * The brew quality from the machine minigame scales the duration, and a Perfect cup gives Speed II.
 */
public class SkofeeItem extends Item {
    private static final String TAG_CUPS = "SkofcraftSkofeeCups";
    private static final String TAG_LAST_CUP = "SkofcraftSkofeeLastCup";
    /** Cups only count toward the jitters if drunk within this many ticks of the previous one. */
    private static final long STREAK_TICKS = 20L * 60 * 5;
    private static final int JITTERS_AFTER = 3;

    /** Brew quality from the SKOFee Machine minigame; cups without it (trades, lattes) brew like a Decent cup. */
    private static final String TAG_QUALITY = "SkofeeQuality";
    public static final int QUALITY_BURNT = 0;
    public static final int QUALITY_DECENT = 1;
    public static final int QUALITY_GOOD = 2;
    public static final int QUALITY_PERFECT = 3;
    private static final String[] QUALITY_KEYS = {"burnt", "decent", "good", "perfect"};
    private static final ChatFormatting[] QUALITY_COLORS = {ChatFormatting.DARK_GRAY, ChatFormatting.GRAY, ChatFormatting.GREEN, ChatFormatting.GOLD};
    private static final float[] QUALITY_DURATION = {0.4F, 1.0F, 1.35F, 1.75F};

    private final int nutrition;
    private final float saturation;
    private final float durationScale;

    public SkofeeItem(Properties properties, int nutrition, float saturation, float durationScale) {
        super(properties);
        this.nutrition = nutrition;
        this.saturation = saturation;
        this.durationScale = durationScale;
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
            int quality = getQuality(stack);
            int ticks = durationSeconds(stack) * 20;
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, quality == QUALITY_PERFECT ? 1 : 0));
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, ticks, 0));
            if (entity instanceof Player player) {
                if (nutrition > 0) {
                    player.getFoodData().eat(nutrition, saturation);
                }
                applyStreak(player, level, "message.skofcraft.skofee_jitters");
            }
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SkofcraftSounds.GULP.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
        }

        if (entity instanceof Player player && player.getAbilities().instabuild) {
            return stack;
        }
        stack.shrink(1);
        ItemStack cup = new ItemStack(SkofcraftItems.SKOFEE_CUP.get());
        if (stack.isEmpty()) {
            return cup;
        }
        if (entity instanceof Player player && !player.getInventory().add(cup)) {
            player.drop(cup, false);
        }
        return stack;
    }

    /** Counts a caffeinated drink (SKOFee or an energy drink) toward the jitters. */
    static void applyStreak(Player player, Level level, String jittersMessage) {
        CompoundTag data = player.getPersistentData();
        long now = level.getGameTime();
        int cups = now - data.getLong(TAG_LAST_CUP) <= STREAK_TICKS ? data.getInt(TAG_CUPS) + 1 : 1;
        data.putInt(TAG_CUPS, cups);
        data.putLong(TAG_LAST_CUP, now);
        if (cups >= JITTERS_AFTER) {
            int extra = cups - JITTERS_AFTER;
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * (8 + extra * 4), 0));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20 * (15 + extra * 5), Math.min(2, extra)));
            player.displayClientMessage(Component.translatable(jittersMessage), true);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    public static int qualityFromScore(int score) {
        if (score >= 90) {
            return QUALITY_PERFECT;
        }
        if (score >= 70) {
            return QUALITY_GOOD;
        }
        return score >= 40 ? QUALITY_DECENT : QUALITY_BURNT;
    }

    public static ItemStack withQuality(ItemStack stack, int quality) {
        stack.getOrCreateTag().putInt(TAG_QUALITY, quality);
        return stack;
    }

    public static int getQuality(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_QUALITY)) {
            return QUALITY_DECENT;
        }
        return Math.max(QUALITY_BURNT, Math.min(QUALITY_PERFECT, tag.getInt(TAG_QUALITY)));
    }

    public static Component qualityName(int quality) {
        return Component.translatable("quality.skofcraft.skofee." + QUALITY_KEYS[quality]).withStyle(QUALITY_COLORS[quality]);
    }

    private int durationSeconds(ItemStack stack) {
        return Math.max(1, Math.round(SkofcraftConfig.skofeeDurationSeconds * durationScale * QUALITY_DURATION[getQuality(stack)]));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int seconds = durationSeconds(stack);
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_QUALITY)) {
            tooltip.add(qualityName(getQuality(stack)));
        }
        tooltip.add(Component.translatable(getQuality(stack) == QUALITY_PERFECT ? "tooltip.skofcraft.skofee_perfect" : "tooltip.skofcraft.skofee",
                seconds / 60, String.format("%02d", seconds % 60)));
    }
}
