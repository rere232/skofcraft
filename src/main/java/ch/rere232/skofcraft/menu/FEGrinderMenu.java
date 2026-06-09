package ch.rere232.skofcraft.menu;

import ch.rere232.skofcraft.blockentity.FEGrinderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class FEGrinderMenu extends AbstractContainerMenu {
    private final FEGrinderBlockEntity blockEntity;
    private final ContainerData data;

    public FEGrinderMenu(int windowId, Inventory playerInventory, FEGrinderBlockEntity blockEntity) {
        this(windowId, playerInventory, blockEntity, new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> blockEntity.getProcessingProgress();
                    case 1 -> blockEntity.getMaxProgress();
                    case 2 -> blockEntity.getEnergy().getEnergyStored();
                    case 3 -> blockEntity.requiresEnergy() ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // server authoritative
            }

            @Override
            public int getCount() {
                return 4;
            }
        });
    }

    private FEGrinderMenu(int windowId, Inventory playerInventory, FEGrinderBlockEntity blockEntity, ContainerData data) {
        super(SkofcraftMenus.FE_GRINDER.get(), windowId);
        this.blockEntity = blockEntity;
        this.data = data;

        addSlot(new Slot(blockEntity.getInputSlots(), 0, 44, 35));
        addSlot(new Slot(blockEntity.getOutputSlots(), 0, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return false;
            }
        });

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int i = 0; i < 9; ++i) {
            addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }

        addDataSlots(data);
    }

    public FEGrinderMenu(int windowId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(windowId, playerInventory, getBlockEntity(playerInventory, buffer), new SimpleContainerData(4));
    }

    private static FEGrinderBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf buffer) {
        if (buffer != null && buffer.isReadable()) {
            BlockPos pos = buffer.readBlockPos();
            BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
            if (blockEntity instanceof FEGrinderBlockEntity grinder) {
                return grinder;
            }
        }
        return new FEGrinderBlockEntity(new BlockPos(0, 0, 0), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);

        if (slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemStack = itemStack1.copy();

            if (slotIndex < 2) {
                if (!moveItemStackTo(itemStack1, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(itemStack1, 0, 2, false)) {
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

    public FEGrinderBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getProgress() {
        return data.get(0);
    }

    public int getMaxProgress() {
        return data.get(1);
    }

    public int getEnergy() {
        return data.get(2);
    }

    public int getMaxEnergy() {
        return blockEntity.getEnergy().getMaxEnergyStored();
    }

    public boolean requiresEnergy() {
        return data.get(3) == 1;
    }
}
