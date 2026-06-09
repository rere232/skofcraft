package ch.rere232.skofcraft.gums;

import ch.rere232.skofcraft.config.SkofcraftConfig;
import ch.rere232.skofcraft.item.GumConsumableItem;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class GumSlotData {
    public static final int SLOT_COUNT = 4;

    private static final String ROOT = "SkofcraftGums";
    private static final String SLOT_PREFIX = "Slot";

    private final Player player;

    public GumSlotData(Player player) {
        this.player = player;
    }

    public static GumSlotData get(Player player) {
        return new GumSlotData(player);
    }

    public ItemStack getSlotItem(int index) {
        CompoundTag rootTag = getRootTag();
        int ticks = rootTag.getInt(SLOT_PREFIX + index);
        if (ticks > 0) {
            return new ItemStack(SkofcraftItems.SNUS_POUCH.get());
        }
        return ItemStack.EMPTY;
    }

    public int getRemainingTicks(int index) {
        CompoundTag rootTag = getRootTag();
        return rootTag.getInt(SLOT_PREFIX + index);
    }

    public void removeSlot(int index) {
        CompoundTag rootTag = getRootTag();
        rootTag.putInt(SLOT_PREFIX + index, 0);
    }

    public void tryInsert(int slotIndex, ItemStack stack) {
        if (!(stack.getItem() instanceof GumConsumableItem consumable)) {
            return;
        }

        CompoundTag rootTag = getRootTag();
        String key = SLOT_PREFIX + slotIndex;
        int ticks = rootTag.getInt(key);
        if (ticks <= 0) {
            int durationTicks = consumable.getDurationMinutes() * 60 * 20;
            rootTag.putInt(key, durationTicks);
        }
    }

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

    private CompoundTag getRootTag() {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT)) {
            persistent.put(ROOT, new CompoundTag());
        }
        return persistent.getCompound(ROOT);
    }

    private static CompoundTag getRootTag(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT)) {
            persistent.put(ROOT, new CompoundTag());
        }
        return persistent.getCompound(ROOT);
    }
}
