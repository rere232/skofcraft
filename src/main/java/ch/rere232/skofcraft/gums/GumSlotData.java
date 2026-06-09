package ch.rere232.skofcraft.gums;

import ch.rere232.skofcraft.config.SkofcraftConfig;
import ch.rere232.skofcraft.item.GumConsumableItem;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class GumSlotData {
    public static final int SLOT_COUNT = 4;

    private static final String ROOT = "SkofcraftGums";
    private static final String SLOT_PREFIX = "Slot";
    private static final String ITEM_PREFIX = "SlotItem";

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
            String itemId = rootTag.getString(ITEM_PREFIX + index);
            ResourceLocation key = ResourceLocation.tryParse(itemId);
            if (key != null) {
                Item item = BuiltInRegistries.ITEM.get(key);
                if (item != net.minecraft.world.item.Items.AIR) {
                    return new ItemStack(item);
                }
            }
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
        rootTag.remove(ITEM_PREFIX + index);
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
            rootTag.putString(ITEM_PREFIX + slotIndex, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
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
                rootTag.putString(ITEM_PREFIX + i, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
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
                int nextTicks = ticks - 1;
                rootTag.putInt(key, nextTicks);
                if (nextTicks <= 0) {
                    rootTag.remove(ITEM_PREFIX + i);
                }
                active++;
            }
        }

        if (!player.level().isClientSide && active > 0) {
            GumStatusEffects.apply(player, active, SkofcraftConfig.enableNegativeEffects);
        }
    }

    public static void applySyncData(Player player, int[] ticks, String[] itemIds) {
        if (ticks == null || itemIds == null || ticks.length < SLOT_COUNT || itemIds.length < SLOT_COUNT) {
            return;
        }
        CompoundTag rootTag = getRootTag(player);
        for (int i = 0; i < SLOT_COUNT; i++) {
            String tickKey = SLOT_PREFIX + i;
            String itemKey = ITEM_PREFIX + i;
            int value = Math.max(0, ticks[i]);
            rootTag.putInt(tickKey, value);
            if (value > 0 && !itemIds[i].isEmpty()) {
                rootTag.putString(itemKey, itemIds[i]);
            } else {
                rootTag.remove(itemKey);
            }
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
