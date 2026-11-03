package ch.rere232.skofcraft.blockentity;

import ch.rere232.skofcraft.item.GumConsumableItem;
import ch.rere232.skofcraft.registry.SkofcraftBlocks;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FEPressBlockEntity extends BlockEntity {
    private static final int CAPACITY = 10000;
    private static final int MAX_RECEIVE = 500;

    private final boolean requiresEnergy;
    private final boolean industrial;
    private final int processingTime;
    private final int energyCost;

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

    private final SimpleContainer inputSlots = new SimpleContainer(3);
    private final SimpleContainer outputSlots = new SimpleContainer(1);
    private final LazyOptional<MachineItemHandler> itemHandler = LazyOptional.of(() -> new MachineItemHandler(inputSlots, outputSlots, this::setChanged, FEPressBlockEntity::isValidInput));

    private int processingProgress = 0;
    private boolean isProcessing = false;
    private int manualWorkTicks = 0;

    public FEPressBlockEntity(BlockPos blockPos, BlockState blockState) {
        this(blockPos, blockState, true);
    }

    public FEPressBlockEntity(BlockPos blockPos, BlockState blockState, boolean requiresEnergy) {
        super(SkofcraftBlockEntities.FE_PRESS.get(), blockPos, blockState);
        this.requiresEnergy = requiresEnergy && !isManualBlock(blockState.getBlock());
        this.industrial = blockState.getBlock() == SkofcraftBlocks.INDUSTRIAL_POUCH_LINE.get();
        this.processingTime = industrial ? 90 : 150;
        this.energyCost = industrial ? 200 : 60;
    }

    private static boolean isManualBlock(Block block) {
        return block == SkofcraftBlocks.MANUAL_POUCH_PRESS.get();
    }

    /** Slot 0 takes the material, slot 1 the empty pouches, slot 2 the optional booster. */
    public static boolean isValidInput(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> stack.is(SkofcraftItems.TOBACCO_DUST.get()) || stack.is(SkofcraftItems.NICOTINE_MIX.get());
            case 1 -> stack.is(SkofcraftItems.EMPTY_POUCH.get());
            case 2 -> stack.is(SkofcraftItems.NICOTINE_EXTRACT.get()) || stack.is(SkofcraftItems.NICOTINE_CRYSTALS.get());
            default -> false;
        };
    }

    public void tick() {
        if (level == null || level.isClientSide) return;

        if (!isProcessing && canProcess()) {
            isProcessing = true;
            processingProgress = 0;
        }

        if (isProcessing) {
            if (requiresEnergy) {
                int energyPerTick = Math.max(1, (int) Math.round(energyCost * ch.rere232.skofcraft.config.SkofcraftConfig.machineEnergyMultiplier));
                if (energy.getEnergyStored() < energyPerTick) {
                    return;
                }
                energy.extractEnergy(energyPerTick, false);
                processingProgress++;
            } else {
                if (manualWorkTicks <= 0) return;
                manualWorkTicks--;
                processingProgress++;
            }

            if (processingProgress >= processingTime) {
                finishProcessing();
                isProcessing = false;
                processingProgress = 0;
            }
            setChanged();
        }
    }

    private boolean canProcess() {
        ItemStack output = outputSlots.getItem(0);
        ItemStack result = buildResult();
        if (result.isEmpty()) return false;

        if (output.isEmpty()) return true;
        return ItemStack.isSameItemSameTags(output, result) && output.getCount() < 64;
    }

    /** Nicotine mix becomes a nicotine pouch; ground tobacco becomes snus, strengthened by the booster slot. */
    private ItemStack buildResult() {
        ItemStack material = inputSlots.getItem(0);
        ItemStack pouch = inputSlots.getItem(1);
        ItemStack booster = inputSlots.getItem(2);

        if (material.isEmpty() || pouch.isEmpty() || pouch.getItem() != SkofcraftItems.EMPTY_POUCH.get()) return ItemStack.EMPTY;

        if (material.getItem() == SkofcraftItems.NICOTINE_MIX.get()) {
            ItemStack result = new ItemStack(SkofcraftItems.NICOTINE_POUCH.get());
            GumConsumableItem.copyTraits(material, result);
            return result;
        }
        if (material.getItem() == SkofcraftItems.TOBACCO_DUST.get()) {
            ItemStack result = new ItemStack(SkofcraftItems.SNUS_POUCH.get());
            GumConsumableItem.setStrength(result, boosterStrength(booster));
            return result;
        }
        return ItemStack.EMPTY;
    }

    /** Extract gives strong snus, one crystal extra strong, two or more crystals premium. */
    private static int boosterStrength(ItemStack booster) {
        if (booster.isEmpty()) return 0;
        if (booster.getItem() == SkofcraftItems.NICOTINE_EXTRACT.get()) return 1;
        if (booster.getItem() == SkofcraftItems.NICOTINE_CRYSTALS.get()) return booster.getCount() >= 2 ? 3 : 2;
        return 0;
    }

    private void finishProcessing() {
        ItemStack material = inputSlots.getItem(0);
        ItemStack pouch = inputSlots.getItem(1);
        ItemStack booster = inputSlots.getItem(2);
        ItemStack output = outputSlots.getItem(0);

        ItemStack result = buildResult();
        if (result.isEmpty()) return;

        if (material.getItem() == SkofcraftItems.TOBACCO_DUST.get()) {
            booster.shrink(boosterStrength(booster) == 3 ? 2 : (boosterStrength(booster) > 0 ? 1 : 0));
        }

        if (output.isEmpty()) {
            outputSlots.setItem(0, result);
        } else if (ItemStack.isSameItemSameTags(output, result) && output.getCount() < 64) {
            output.grow(1);
        }

        material.shrink(1);
        pouch.shrink(1);
    }

    public boolean manualCrank() {
        if (requiresEnergy || !canProcess()) {
            return false;
        }
        if (!isProcessing) {
            isProcessing = true;
            processingProgress = 0;
        }
        manualWorkTicks = Math.min(manualWorkTicks + 20, processingTime);
        setChanged();
        return true;
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        int savedEnergy = tag.getInt("Energy");
        energy.extractEnergy(energy.getMaxEnergyStored(), false);
        energy.receiveEnergy(savedEnergy, false);
        processingProgress = tag.getInt("Progress");
        isProcessing = tag.getBoolean("Processing");
        manualWorkTicks = tag.getInt("ManualWorkTicks");

        net.minecraft.nbt.ListTag inputList = tag.getList("InputItems", 10);
        for (int i = 0; i < inputSlots.getContainerSize(); i++) {
            inputSlots.setItem(i, ItemStack.EMPTY);
        }
        for (int i = 0; i < inputList.size() && i < inputSlots.getContainerSize(); i++) {
            CompoundTag stackTag = inputList.getCompound(i);
            // Saves from before slot indices were stored fall back to list order
            int slot = stackTag.contains("Slot") ? stackTag.getByte("Slot") : i;
            if (slot >= 0 && slot < inputSlots.getContainerSize()) {
                inputSlots.setItem(slot, ItemStack.of(stackTag));
            }
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
        tag.putInt("ManualWorkTicks", manualWorkTicks);

        net.minecraft.nbt.ListTag inputList = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < inputSlots.getContainerSize(); i++) {
            ItemStack stack = inputSlots.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag stackTag = new CompoundTag();
                stackTag.putByte("Slot", (byte) i);
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

    public boolean requiresEnergy() {
        return requiresEnergy;
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

    public boolean isProcessing() {
        return isProcessing;
    }

    public boolean isIndustrial() {
        return industrial;
    }
}
