package ch.rere232.skofcraft.blockentity;

import ch.rere232.skofcraft.item.GumConsumableItem;
import ch.rere232.skofcraft.registry.SkofcraftBlocks;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
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

public class FEMixerBlockEntity extends BlockEntity {
    private static final int CAPACITY = 10000;
    private static final int MAX_RECEIVE = 100;
    private static final int ENERGY_PER_TICK = 20;

    private final boolean requiresEnergy;
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

    private final Container inputSlots = new SimpleContainer(3);
    private final Container outputSlots = new SimpleContainer(1);
    private final LazyOptional<MachineItemHandler> itemHandler = LazyOptional.of(() -> new MachineItemHandler(inputSlots, outputSlots, this::setChanged));

    private int processingProgress = 0;
    private boolean isProcessing = false;
    private int manualWorkTicks = 0;

    public FEMixerBlockEntity(BlockPos blockPos, BlockState blockState) {
        this(blockPos, blockState, true);
    }

    public FEMixerBlockEntity(BlockPos blockPos, BlockState blockState, boolean requiresEnergy) {
        super(SkofcraftBlockEntities.FE_MIXER.get(), blockPos, blockState);
        this.requiresEnergy = requiresEnergy && !isManualBlock(blockState.getBlock());
        this.industrial = blockState.getBlock() == SkofcraftBlocks.INDUSTRIAL_SNUS_LINE.get();
        this.processingTime = industrial ? 120 : 200;
    }

    private static boolean isManualBlock(Block block) {
        return block == SkofcraftBlocks.MANUAL_MIXER.get();
    }

    public void tick() {
        if (level == null || level.isClientSide) return;

        if (!isProcessing && canProcess()) {
            isProcessing = true;
            processingProgress = 0;
        }

        if (isProcessing) {
            if (requiresEnergy) {
                if (energy.getEnergyStored() < ENERGY_PER_TICK) {
                    return;
                }
                energy.extractEnergy(ENERGY_PER_TICK, false);
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
        ItemStack extract = inputSlots.getItem(0);
        ItemStack flavor = inputSlots.getItem(1);
        ItemStack pouch = inputSlots.getItem(2);
        ItemStack output = outputSlots.getItem(0);

        if (extract.isEmpty() || extract.getItem() != SkofcraftItems.NICOTINE_EXTRACT.get()) return false;
        if (flavor.isEmpty() || !isFlavorItem(flavor.getItem())) return false;
        if (pouch.isEmpty() || pouch.getItem() != SkofcraftItems.EMPTY_POUCH.get()) return false;

        if (output.isEmpty()) return true;
        return output.getCount() < 64;
    }

    private void finishProcessing() {
        ItemStack extract = inputSlots.getItem(0);
        ItemStack flavor = inputSlots.getItem(1);
        ItemStack pouch = inputSlots.getItem(2);
        ItemStack output = outputSlots.getItem(0);

        if (extract.isEmpty() || flavor.isEmpty() || pouch.isEmpty()) return;

        ItemStack result = new ItemStack(SkofcraftItems.NICOTINE_POUCH.get());
        GumConsumableItem.setFlavor(result, flavor);

        if (output.isEmpty()) {
            outputSlots.setItem(0, result);
        } else if (output.getCount() < 64) {
            output.grow(1);
        }

        extract.shrink(1);
        flavor.shrink(1);
        pouch.shrink(1);
    }

    private boolean isFlavorItem(Object item) {
        return item == SkofcraftItems.FLAVOR_MINT.get()
                || item == SkofcraftItems.FLAVOR_ICE_MINT.get()
                || item == SkofcraftItems.FLAVOR_BERRY.get()
                || item == SkofcraftItems.FLAVOR_CITRUS.get()
                || item == SkofcraftItems.FLAVOR_LICORICE.get()
                || item == SkofcraftItems.FLAVOR_COLA.get()
                || item == SkofcraftItems.FLAVOR_COFFEE.get()
                || item == SkofcraftItems.FLAVOR_NATURE.get();
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
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("Progress", processingProgress);
        tag.putBoolean("Processing", isProcessing);
        tag.putInt("ManualWorkTicks", manualWorkTicks);
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
