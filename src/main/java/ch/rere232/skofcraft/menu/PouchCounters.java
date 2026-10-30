package ch.rere232.skofcraft.menu;

import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Click handling for machine slots that hold many pouches as one counter.
 * Pouches do not stack in inventories, so these slots move them one at a time.
 */
final class PouchCounters {
    static final int MAX_COUNT = 64;

    private PouchCounters() {}

    static boolean isPouch(ItemStack stack) {
        return stack.is(SkofcraftItems.SNUS_POUCH.get()) || stack.is(SkofcraftItems.NICOTINE_POUCH.get());
    }

    /** Handles pickup, shift-click and drop; ignores hotbar swaps. Other click types go to vanilla. */
    static boolean handleClick(AbstractContainerMenu menu, Slot slot, ClickType type, Player player, boolean acceptsInput) {
        switch (type) {
            case PICKUP -> {
                ItemStack carried = menu.getCarried();
                if (carried.isEmpty()) {
                    ItemStack one = takeOne(slot);
                    if (!one.isEmpty()) {
                        menu.setCarried(one);
                    }
                } else if (acceptsInput) {
                    insertOne(slot, carried);
                }
                return true;
            }
            case QUICK_MOVE -> {
                moveAllToInventory(slot, player);
                return true;
            }
            case THROW -> {
                ItemStack one = takeOne(slot);
                if (!one.isEmpty()) {
                    player.drop(one, true);
                }
                return true;
            }
            case SWAP -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /** Moves one pouch from the given stack into the counter slot. */
    static boolean insertOne(Slot slot, ItemStack from) {
        if (from.isEmpty() || !isPouch(from)) {
            return false;
        }
        ItemStack current = slot.getItem();
        if (current.isEmpty()) {
            slot.set(from.split(1));
            return true;
        }
        if (ItemStack.isSameItemSameTags(current, from) && current.getCount() < MAX_COUNT) {
            current.grow(1);
            from.shrink(1);
            slot.setChanged();
            return true;
        }
        return false;
    }

    private static ItemStack takeOne(Slot slot) {
        ItemStack current = slot.getItem();
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack one = current.copyWithCount(1);
        current.shrink(1);
        if (current.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return one;
    }

    private static void moveAllToInventory(Slot slot, Player player) {
        while (!slot.getItem().isEmpty()) {
            ItemStack one = slot.getItem().copyWithCount(1);
            if (!player.getInventory().add(one)) {
                return;
            }
            ItemStack current = slot.getItem();
            current.shrink(1);
            if (current.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
    }
}
