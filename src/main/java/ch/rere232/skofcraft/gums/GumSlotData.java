package ch.rere232.skofcraft.gums;

import ch.rere232.skofcraft.config.SkofcraftConfig;
import ch.rere232.skofcraft.item.GumConsumableItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class GumSlotData {
    public static final int SLOT_COUNT = 4;

    private static final String ROOT = "SkofcraftGums";
    private static final String SLOT_PREFIX = "Slot";

    public static boolean tryInsert(Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof GumConsumableItem consumable)) {
            return false;
        }

        CompoundTag rootTag = getRootTag(player);
        for (int i = 0; i < SLOT_COUNT; i++) {
            String key = SLOT_PREFIX + i;
            int ticks = rootTag.getInt(key);
            if (ticks <= 0) {
                int durationTicks = consumable.getDurationMinutes() * 60 * 20;
                rootTag.putInt(key, durationTicks);
                return true;
            }
        }
        return false;
    }

    public static void tick(Player player) {
        CompoundTag rootTag = getRootTag(player);
        int active = 0;
        for (int i = 0; i < SLOT_COUNT; i++) {
            String key = SLOT_PREFIX + i;
            int ticks = rootTag.getInt(key);
            if (ticks > 0) {
                rootTag.putInt(key, ticks - 1);
                active++;
            }
        }

        if (active > 0) {
            GumStatusEffects.apply(player, active, SkofcraftConfig.enableNegativeEffects);
        }
    }

    private static CompoundTag getRootTag(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT)) {
            persistent.put(ROOT, new CompoundTag());
        }
        return persistent.getCompound(ROOT);
    }
}
