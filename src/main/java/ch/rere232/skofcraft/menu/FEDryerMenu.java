package ch.rere232.skofcraft.menu;

import ch.rere232.skofcraft.blockentity.FEDryerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class FEDryerMenu extends AbstractContainerMenu {
    public static final int CONTAINER_SIZE = 2;
    public static final int SLOT_COUNT = 2;

    private final FEDryerBlockEntity blockEntity;
    private final Container inputSlots;
    private final Container outputSlots;
    private final ContainerData data;

    public FEDryerMenu(int windowId, Inventory playerInventory, FEDryerBlockEntity blockEntity) {
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

    private FEDryerMenu(int windowId, Inventory playerInventory, FEDryerBlockEntity blockEntity, ContainerData data) {
        super(SkofcraftMenus.FE_DRYER.get(), windowId);
        this.blockEntity = blockEntity;
        this.inputSlots = blockEntity.getInputSlots();
        this.outputSlots = blockEntity.getOutputSlots();
        this.data = data;

        addSlot(new Slot(inputSlots, 0, 44, 35));
        addSlot(new Slot(outputSlots, 0, 116, 35) {
            @Override
            public boolean mayPlace(@NotNull ItemStack itemStack) {
                return false;
            }
        });

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addDataSlots(data);
    }

    public FEDryerMenu(int windowId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(windowId, playerInventory, getBlockEntity(playerInventory, buffer), new SimpleContainerData(4));
    }

    private static FEDryerBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf buffer) {
        if (buffer != null && buffer.isReadable()) {
            BlockPos pos = buffer.readBlockPos();
            BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
            if (blockEntity instanceof FEDryerBlockEntity dryer) {
                return dryer;
            }
        }
        return new FEDryerBlockEntity(new BlockPos(0, 0, 0), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @NotNull
    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int slot) {
        ItemStack result = ItemStack.EMPTY;
        Slot slotObject = slots.get(slot);

        if (slotObject.hasItem()) {
            ItemStack slotStack = slotObject.getItem();
            result = slotStack.copy();

            if (slot < SLOT_COUNT) {
                if (!moveItemStackTo(slotStack, SLOT_COUNT, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(slotStack, 0, SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slotObject.set(ItemStack.EMPTY);
            } else {
                slotObject.setChanged();
            }
        }

        return result;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    public FEDryerBlockEntity getBlockEntity() {
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
