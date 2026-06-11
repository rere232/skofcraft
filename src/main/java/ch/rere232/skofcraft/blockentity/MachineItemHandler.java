package ch.rere232.skofcraft.blockentity;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

public class MachineItemHandler implements IItemHandlerModifiable {
    private final Container inputSlots;
    private final Container outputSlots;
    private final Runnable onChange;

    public MachineItemHandler(Container inputSlots, Container outputSlots, Runnable onChange) {
        this.inputSlots = inputSlots;
        this.outputSlots = outputSlots;
        this.onChange = onChange;
    }

    @Override
    public int getSlots() {
        return inputSlots.getContainerSize() + outputSlots.getContainerSize();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return getContainerForSlot(slot).getItem(getContainerSlot(slot));
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || isOutputSlot(slot)) {
            return stack;
        }

        Container container = getContainerForSlot(slot);
        int containerSlot = getContainerSlot(slot);
        ItemStack existing = container.getItem(containerSlot);
        int limit = Math.min(container.getMaxStackSize(), stack.getMaxStackSize());

        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameTags(existing, stack)) {
                return stack;
            }
            limit -= existing.getCount();
        }

        if (limit <= 0) {
            return stack;
        }

        int toInsert = Math.min(limit, stack.getCount());
        if (!simulate) {
            if (existing.isEmpty()) {
                container.setItem(containerSlot, stack.copyWithCount(toInsert));
            } else {
                existing.grow(toInsert);
                container.setItem(containerSlot, existing);
            }
            onChange.run();
        }

        if (stack.getCount() == toInsert) {
            return ItemStack.EMPTY;
        }
        return stack.copyWithCount(stack.getCount() - toInsert);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0 || isInputSlot(slot)) {
            return ItemStack.EMPTY;
        }

        Container container = getContainerForSlot(slot);
        int containerSlot = getContainerSlot(slot);
        ItemStack existing = container.getItem(containerSlot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int toExtract = Math.min(amount, existing.getCount());
        ItemStack result = existing.copyWithCount(toExtract);
        if (!simulate) {
            existing.shrink(toExtract);
            container.setItem(containerSlot, existing);
            onChange.run();
        }
        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        return Math.min(getContainerForSlot(slot).getMaxStackSize(), 64);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return !isOutputSlot(slot) && !stack.isEmpty();
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (isOutputSlot(slot)) {
            return;
        }
        Container container = getContainerForSlot(slot);
        container.setItem(getContainerSlot(slot), stack);
        onChange.run();
    }

    private boolean isInputSlot(int slot) {
        return slot >= 0 && slot < inputSlots.getContainerSize();
    }

    private boolean isOutputSlot(int slot) {
        return slot >= inputSlots.getContainerSize() && slot < getSlots();
    }

    private Container getContainerForSlot(int slot) {
        if (isInputSlot(slot)) {
            return inputSlots;
        }
        if (isOutputSlot(slot)) {
            return outputSlots;
        }
        throw new IndexOutOfBoundsException("Slot " + slot + " out of range");
    }

    private int getContainerSlot(int slot) {
        if (isInputSlot(slot)) {
            return slot;
        }
        if (isOutputSlot(slot)) {
            return slot - inputSlots.getContainerSize();
        }
        throw new IndexOutOfBoundsException("Slot " + slot + " out of range");
    }
}
