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

    public static void apply(Player player, GumSlotData data, boolean enableNegativeEffects) {
        int activeSlots = 0;
        for (int i = 0; i < GumSlotData.SLOT_COUNT; i++) {
            if (data.getRemainingTicks(i) > 0) {
                activeSlots++;
            }
        }

        if (activeSlots <= 0) {
            return;
        }

        int positiveAmplifier = Math.min(1, activeSlots - 1);

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, positiveAmplifier, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120, positiveAmplifier, true, false, true));

        for (int i = 0; i < GumSlotData.SLOT_COUNT; i++) {
            if (data.getRemainingTicks(i) <= 0) {
                continue;
            }
            ItemStack slotItem = data.getSlotItem(i);
            if (slotItem.getItem() != SkofcraftItems.NICOTINE_POUCH.get()) {
                continue;
            }

            String flavor = GumConsumableItem.getFlavorId(slotItem);
            applyFlavorEffect(player, flavor);
        }

        if (enableNegativeEffects && activeSlots >= 3) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, true, false, true));
        }
        if (enableNegativeEffects && activeSlots >= 4) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0, true, false, true));
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
