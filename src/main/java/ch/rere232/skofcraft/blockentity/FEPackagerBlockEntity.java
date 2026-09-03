package ch.rere232.skofcraft.blockentity;

import ch.rere232.skofcraft.registry.SkofcraftBlocks;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import ch.rere232.skofcraft.config.SkofcraftConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FEPackagerBlockEntity extends BlockEntity {
    private static final int CAPACITY = 10000;
    private static final int MAX_RECEIVE = 100;
    private static final int ENERGY_PER_TICK = 20;

    private final boolean industrial;
    private final int processingTime;

    private final EnergyStorage energy = new EnergyStorage(CAPACITY, MAX_RECEIVE, 0, 0) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
            }
            return received;
        }
    };
    private final LazyOptional<EnergyStorage> energyHandler = LazyOptional.of(() -> energy);

    private final SimpleContainer inputSlots = new SimpleContainer(2);
    private final SimpleContainer outputSlots = new SimpleContainer(1);
    private final LazyOptional<MachineItemHandler> itemHandler = LazyOptional.of(() -> new MachineItemHandler(inputSlots, outputSlots, this::setChanged, (slot, stack) -> slot == 0 ? stack.is(SkofcraftItems.SNUS_POUCH.get()) || stack.is(SkofcraftItems.NICOTINE_POUCH.get()) : slot == 1 && (stack.is(SkofcraftItems.EMPTY_SNUS_BOX.get()) || stack.is(SkofcraftItems.EMPTY_POUCH_BOX.get()))));

    private int processingProgress = 0;
    private boolean isProcessing = false;

    public FEPackagerBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(SkofcraftBlockEntities.FE_PACKAGER.get(), blockPos, blockState);
        this.industrial = blockState.getBlock() == SkofcraftBlocks.INDUSTRIAL_PACKAGER.get();
        this.processingTime = industrial ? 80 : 160;
    }

    public void tick() {
        if (level == null || level.isClientSide) return;

        if (!isProcessing && canProcess()) {
            isProcessing = true;
            processingProgress = 0;
        }

        if (isProcessing) {
            int energyPerTick = Math.max(1, (int) Math.round(ENERGY_PER_TICK * ch.rere232.skofcraft.config.SkofcraftConfig.machineEnergyMultiplier));
            if (energy.getEnergyStored() < energyPerTick) return;
            energy.extractEnergy(energyPerTick, false);
            processingProgress++;

            if (processingProgress >= processingTime) {
                finishProcessing();
                isProcessing = false;
                processingProgress = 0;
            }
            setChanged();
        }
    }

    private boolean canProcess() {
        ItemStack pouchStack = inputSlots.getItem(0);
        ItemStack box = inputSlots.getItem(1);
        ItemStack output = outputSlots.getItem(0);
        ItemStack result = getResultForInputs(pouchStack, box);

            int capacity = Math.max(1, SkofcraftConfig.boxCapacity);
            if (pouchStack.isEmpty() || pouchStack.getCount() < capacity || box.isEmpty() || result.isEmpty()) return false;
        if (output.isEmpty()) return true;
        return output.getItem() == result.getItem() && output.getCount() < 64;
    }

    private void finishProcessing() {
        ItemStack pouchStack = inputSlots.getItem(0);
        ItemStack box = inputSlots.getItem(1);
        ItemStack output = outputSlots.getItem(0);
        ItemStack result = getResultForInputs(pouchStack, box);

            int capacity = Math.max(1, SkofcraftConfig.boxCapacity);
            if (pouchStack.isEmpty() || pouchStack.getCount() < capacity || box.isEmpty() || result.isEmpty()) return;

        if (output.isEmpty()) {
            outputSlots.setItem(0, result.copy());
        } else if (output.getItem() == result.getItem() && output.getCount() < 64) {
            output.grow(1);
        }

            pouchStack.shrink(capacity);
        box.shrink(1);
    }

    private ItemStack getResultForInputs(ItemStack pouchStack, ItemStack box) {
        if (pouchStack.getItem() == SkofcraftItems.SNUS_POUCH.get() && box.getItem() == SkofcraftItems.EMPTY_SNUS_BOX.get()) {
            return new ItemStack(SkofcraftItems.FILLED_SNUS_BOX.get());
        }
        if (pouchStack.getItem() == SkofcraftItems.NICOTINE_POUCH.get() && box.getItem() == SkofcraftItems.EMPTY_POUCH_BOX.get()) {
            return new ItemStack(SkofcraftItems.FILLED_POUCH_BOX.get());
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        int savedEnergy = tag.getInt("Energy");
        energy.extractEnergy(energy.getMaxEnergyStored(), false);
        energy.receiveEnergy(savedEnergy, false);
        processingProgress = tag.getInt("Progress");
        isProcessing = tag.getBoolean("Processing");

        net.minecraft.nbt.ListTag inputList = tag.getList("InputItems", 10);
        for (int i = 0; i < inputSlots.getContainerSize(); i++) {
            inputSlots.setItem(i, ItemStack.EMPTY);
        }
        for (int i = 0; i < inputList.size() && i < inputSlots.getContainerSize(); i++) {
            inputSlots.setItem(i, ItemStack.of(inputList.getCompound(i)));
        }

        net.minecraft.nbt.ListTag outputList = tag.getList("OutputItems", 10);
        for (int i = 0; i < outputSlots.getContainerSize(); i++) {
            outputSlots.setItem(i, ItemStack.EMPTY);
        }
        for (int i = 0; i < outputList.size() && i < outputSlots.getContainerSize(); i++) {
            outputSlots.setItem(i, ItemStack.of(outputList.getCompound(i)));
        }
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("Progress", processingProgress);
        tag.putBoolean("Processing", isProcessing);

        net.minecraft.nbt.ListTag inputList = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < inputSlots.getContainerSize(); i++) {
            ItemStack stack = inputSlots.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag stackTag = new CompoundTag();
                stack.save(stackTag);
                inputList.add(stackTag);
            }
        }
        tag.put("InputItems", inputList);

        net.minecraft.nbt.ListTag outputList = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < outputSlots.getContainerSize(); i++) {
            ItemStack stack = outputSlots.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag stackTag = new CompoundTag();
                stack.save(stackTag);
                outputList.add(stackTag);
            }
        }
        tag.put("OutputItems", outputList);
    }

    @Override
    public @Nullable ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyHandler.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyHandler.invalidate();
        itemHandler.invalidate();
    }

    public EnergyStorage getEnergy() {
        return energy;
    }

    public Container getInputSlots() {
        return inputSlots;
    }

    public Container getOutputSlots() {
        return outputSlots;
    }

    public int getProcessingProgress() {
        return processingProgress;
    }

    public int getMaxProgress() {
        return processingTime;
    }

    public boolean isIndustrial() {
        return industrial;
    }
}
