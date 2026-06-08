package ch.rere232.skofcraft.menu;

import ch.rere232.skofcraft.blockentity.FEPressBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FEPressMenu extends AbstractContainerMenu {
    private final FEPressBlockEntity blockEntity;

    public FEPressMenu(int windowId, Inventory playerInventory, FEPressBlockEntity blockEntity) {
        super(SkofcraftMenus.FE_PRESS.get(), windowId);
        this.blockEntity = blockEntity;

        addSlot(new Slot(blockEntity.getInputSlots(), 0, 44, 35));
        addSlot(new Slot(blockEntity.getInputSlots(), 1, 71, 35));
        addSlot(new Slot(blockEntity.getOutputSlots(), 0, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return false;
            }
        });

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 140 + i * 18));
            }
        }

        for (int i = 0; i < 9; ++i) {
            addSlot(new Slot(playerInventory, i, 8 + i * 18, 198));
        }
    }

    public FEPressMenu(int windowId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(windowId, playerInventory, new FEPressBlockEntity(new net.minecraft.core.BlockPos(0, 0, 0), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);

        if (slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemStack = itemStack1.copy();

            if (slotIndex < 3) {
                if (!moveItemStackTo(itemStack1, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(itemStack1, 0, 3, false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public FEPressBlockEntity getBlockEntity() {
        return blockEntity;
    }
}
