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
    private static final String FLAVOR_PREFIX = "SlotFlavor";
    private static final String TOLERANCE_KEY = "Tolerance";
    private static final String ADDICTION_KEY = "Addiction";
    private static final String CRAVING_TICKS_KEY = "CravingTicks";

    private static final int MAX_TOLERANCE = 100;
    private static final int MAX_ADDICTION = 100;

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
                    ItemStack stack = new ItemStack(item);
                    String flavorId = rootTag.getString(FLAVOR_PREFIX + index);
                    if (!flavorId.isEmpty()) {
                        stack.getOrCreateTag().putString(GumConsumableItem.TAG_FLAVOR, flavorId);
                    }
                    return stack;
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
        rootTag.remove(FLAVOR_PREFIX + index);
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
            String flavorId = GumConsumableItem.getFlavorId(stack);
            if (!flavorId.isEmpty()) {
                rootTag.putString(FLAVOR_PREFIX + slotIndex, flavorId);
            }
            onProductInserted(rootTag);
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
                String flavorId = GumConsumableItem.getFlavorId(stack);
                if (!flavorId.isEmpty()) {
                    rootTag.putString(FLAVOR_PREFIX + i, flavorId);
                }
                onProductInserted(rootTag);
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
                    rootTag.remove(FLAVOR_PREFIX + i);
                }
                active++;
            }
        }

        if (!player.level().isClientSide) {
            if (active > 0) {
                rootTag.putInt(CRAVING_TICKS_KEY, 0);
                if (SkofcraftConfig.enableTolerance && player.tickCount % 200 == 0) {
                    int tolerance = rootTag.getInt(TOLERANCE_KEY);
                    rootTag.putInt(TOLERANCE_KEY, Math.min(MAX_TOLERANCE, tolerance + 1));
                }
                GumStatusEffects.apply(player, get(player), SkofcraftConfig.enableNegativeEffects, SkofcraftConfig.enableTolerance, rootTag.getInt(TOLERANCE_KEY));
                return;
            }

            if (SkofcraftConfig.enableTolerance && player.tickCount % 200 == 0) {
                int tolerance = rootTag.getInt(TOLERANCE_KEY);
                rootTag.putInt(TOLERANCE_KEY, Math.max(0, tolerance - 1));
            }

            if (SkofcraftConfig.enableAddiction) {
                int addiction = rootTag.getInt(ADDICTION_KEY);
                int cravingTicks = rootTag.getInt(CRAVING_TICKS_KEY) + 1;
                rootTag.putInt(CRAVING_TICKS_KEY, cravingTicks);

                if (player.tickCount % 1200 == 0) {
                    rootTag.putInt(ADDICTION_KEY, Math.max(0, addiction - 1));
                }

                GumStatusEffects.applyCraving(player, addiction, cravingTicks, SkofcraftConfig.enableNegativeEffects);
            } else {
                rootTag.putInt(CRAVING_TICKS_KEY, 0);
            }
        }
    }

    private static void onProductInserted(CompoundTag rootTag) {
        rootTag.putInt(CRAVING_TICKS_KEY, 0);

        if (SkofcraftConfig.enableTolerance) {
            int tolerance = rootTag.getInt(TOLERANCE_KEY);
            rootTag.putInt(TOLERANCE_KEY, Math.min(MAX_TOLERANCE, tolerance + 2));
        }

        if (SkofcraftConfig.enableAddiction) {
            int addiction = rootTag.getInt(ADDICTION_KEY);
            rootTag.putInt(ADDICTION_KEY, Math.min(MAX_ADDICTION, addiction + 3));
        }
    }

    public static void applySyncData(Player player, int[] ticks, String[] itemIds, String[] flavorIds) {
        if (ticks == null || itemIds == null || flavorIds == null || ticks.length < SLOT_COUNT || itemIds.length < SLOT_COUNT || flavorIds.length < SLOT_COUNT) {
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
                if (!flavorIds[i].isEmpty()) {
                    rootTag.putString(FLAVOR_PREFIX + i, flavorIds[i]);
                } else {
                    rootTag.remove(FLAVOR_PREFIX + i);
                }
            } else {
                rootTag.remove(itemKey);
                rootTag.remove(FLAVOR_PREFIX + i);
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
