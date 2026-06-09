package ch.rere232.skofcraft.menu;

import ch.rere232.skofcraft.blockentity.FEMixerBlockEntity;
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

public class FEMixerMenu extends AbstractContainerMenu {
    private final FEMixerBlockEntity blockEntity;
    private final ContainerData data;

    public FEMixerMenu(int windowId, Inventory playerInventory, FEMixerBlockEntity blockEntity) {
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

    private FEMixerMenu(int windowId, Inventory playerInventory, FEMixerBlockEntity blockEntity, ContainerData data) {
        super(SkofcraftMenus.FE_MIXER.get(), windowId);
        this.blockEntity = blockEntity;
        this.data = data;

        addSlot(new Slot(blockEntity.getInputSlots(), 0, 26, 35));
        addSlot(new Slot(blockEntity.getInputSlots(), 1, 44, 35));
        addSlot(new Slot(blockEntity.getInputSlots(), 2, 62, 35));
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

    public FEMixerMenu(int windowId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(windowId, playerInventory, getBlockEntity(playerInventory, buffer), new SimpleContainerData(4));
    }

    private static FEMixerBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf buffer) {
        if (buffer != null && buffer.isReadable()) {
            BlockPos pos = buffer.readBlockPos();
            BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
            if (blockEntity instanceof FEMixerBlockEntity mixer) {
                return mixer;
            }
        }
        return new FEMixerBlockEntity(new BlockPos(0, 0, 0), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);

        if (slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemStack = itemStack1.copy();

            if (slotIndex < 4) {
                if (!moveItemStackTo(itemStack1, 4, 40, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(itemStack1, 0, 4, false)) {
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

    public FEMixerBlockEntity getBlockEntity() {
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
