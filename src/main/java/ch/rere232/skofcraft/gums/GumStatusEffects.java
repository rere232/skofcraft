package ch.rere232.skofcraft.gums;

import ch.rere232.skofcraft.item.GumConsumableItem;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class GumStatusEffects {
    private GumStatusEffects() {
    }

    public static void apply(Player player, GumSlotData data, boolean enableNegativeEffects, boolean enableTolerance, int toleranceLevel) {
        int activeSlots = 0;
        int maxStrength = 0;
        for (int i = 0; i < GumSlotData.SLOT_COUNT; i++) {
            if (data.getRemainingTicks(i) > 0) {
                activeSlots++;
                maxStrength = Math.max(maxStrength, GumConsumableItem.getStrength(data.getSlotItem(i)));
            }
        }

        if (activeSlots <= 0) {
            return;
        }

        int baseAmplifier = Math.min(1, activeSlots - 1);
        int tolerancePenalty = enableTolerance ? toleranceLevel / 40 : 0;
        int positiveAmplifier = Math.max(0, baseAmplifier + Math.min(2, maxStrength) - tolerancePenalty);
        boolean premium = maxStrength >= 3;

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, positiveAmplifier, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120, positiveAmplifier, true, false, true));

        for (int i = 0; i < GumSlotData.SLOT_COUNT; i++) {
            if (data.getRemainingTicks(i) <= 0) {
                continue;
            }
            ItemStack slotItem = data.getSlotItem(i);
            if (slotItem.getItem() == SkofcraftItems.CHEWING_TOBACCO.get()) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
                continue;
            }
            if (slotItem.getItem() != SkofcraftItems.NICOTINE_POUCH.get()) {
                continue;
            }

            String flavor = GumConsumableItem.getFlavorId(slotItem);
            applyFlavorEffect(player, flavor);
        }

        if (enableNegativeEffects && !premium && activeSlots >= 3) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, true, false, true));
        }
        if (enableNegativeEffects && !premium && activeSlots >= 4) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0, true, false, true));
        }
    }

    /** One puff from a chicha: a longer, steady boost plus the loaded flavor's effect. */
    public static void applyChicha(Player player, String flavorId) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 600, 0, true, false, true));
        applyFlavorEffect(player, flavorId);
    }

    /** Smoked tobacco: a long boost with a weakening cost when negative effects are on. */
    public static void applySmoke(Player player, int effectTicks, int amplifier, boolean resistance, int weaknessTicks, boolean enableNegativeEffects) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, effectTicks, amplifier, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, effectTicks, amplifier, true, false, true));
        if (resistance) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, effectTicks, 0, true, false, true));
        }
        if (enableNegativeEffects) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, weaknessTicks, 0, true, false, true));
        }
    }

    /** One puff from a vape: a lighter, shorter boost than snuff, scaled by the liquid's strength. */
    public static void applyVape(Player player, String flavorId, int strength) {
        int amplifier = Math.min(2, strength);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, amplifier, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 400, amplifier, true, false, true));
        applyFlavorEffect(player, flavorId);
    }

    /** Short, strong burst from a pinch of snuff, plus the flavor's own effect. */
    public static void applySnuff(Player player, String flavorId, boolean enableNegativeEffects) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 600, 1, true, false, true));
        applyFlavorEffect(player, flavorId);
        if (enableNegativeEffects) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, true, false, true));
        }
    }

    private static final int WITHDRAWAL_THRESHOLD = 70;

    public static void applyCraving(Player player, int addictionLevel, int cravingTicks, boolean enableNegativeEffects) {
        if (!enableNegativeEffects || addictionLevel <= WITHDRAWAL_THRESHOLD || cravingTicks < 20 * 30) {
            return;
        }

        int stage = 0;
        if (cravingTicks >= 20 * 180) {
            stage = 3;
        } else if (cravingTicks >= 20 * 90) {
            stage = 2;
        } else {
            stage = 1;
        }

        int severity = Math.min(2, (addictionLevel - WITHDRAWAL_THRESHOLD) / 10);
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, Math.min(1 + severity, 2), true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, severity, true, false, true));

        if (stage >= 2) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, severity, true, false, true));
        }
        if (stage >= 3) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, true, false, true));
        }
    }

    private static void applyFlavorEffect(Player player, String flavorId) {
        if (flavorId == null || flavorId.isEmpty()) {
            return;
        }

        if (flavorId.endsWith("flavor_mint")) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0, true, false, true));
            return;
        }
        if (flavorId.endsWith("flavor_ice_mint")) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120, 1, true, false, true));
            return;
        }
        if (flavorId.endsWith("flavor_berry")) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, false, true));
            return;
        }
        if (flavorId.endsWith("flavor_citrus")) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120, 1, true, false, true));
            return;
        }
        if (flavorId.endsWith("flavor_licorice")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0, true, false, true));
            return;
        }
        if (flavorId.endsWith("flavor_cola")) {
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 120, 0, true, false, true));
            return;
        }
        if (flavorId.endsWith("flavor_coffee")) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120, 1, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0, true, false, true));
            return;
        }
        if (flavorId.endsWith("flavor_nature")) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 120, 0, true, false, true));
        }
    }
}
