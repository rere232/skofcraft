package ch.rere232.skofcraft.blockentity;

import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
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

public class FEMixerBlockEntity extends BlockEntity {
    private static final int CAPACITY = 10000;
    private static final int MAX_RECEIVE = 100;
    private static final int PROCESSING_TIME = 200;
    private static final int ENERGY_PER_TICK = 20;

    private final boolean requiresEnergy;

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

    private int processingProgress = 0;
    private boolean isProcessing = false;

    public FEMixerBlockEntity(BlockPos blockPos, BlockState blockState) {
        this(blockPos, blockState, true);
    }

    public FEMixerBlockEntity(BlockPos blockPos, BlockState blockState, boolean requiresEnergy) {
        super(SkofcraftBlockEntities.FE_MIXER.get(), blockPos, blockState);
        this.requiresEnergy = requiresEnergy;
    }

    public void tick() {
        if (level == null || level.isClientSide) return;

        if (!isProcessing && canProcess()) {
            isProcessing = true;
            processingProgress = 0;
        }

        if (isProcessing) {
            if (!requiresEnergy || energy.getEnergyStored() >= ENERGY_PER_TICK) {
                if (requiresEnergy) energy.extractEnergy(ENERGY_PER_TICK, false);
                processingProgress++;

                if (processingProgress >= PROCESSING_TIME) {
                    finishProcessing();
                    isProcessing = false;
                    processingProgress = 0;
                }
                setChanged();
            }
        }
    }

    private boolean canProcess() {
        ItemStack dust = inputSlots.getItem(0);
        ItemStack flavor = inputSlots.getItem(1);
        ItemStack salt = inputSlots.getItem(2);
        ItemStack output = outputSlots.getItem(0);

        if (dust.isEmpty() || dust.getItem() != SkofcraftItems.TOBACCO_DUST.get()) return false;
        if (flavor.isEmpty()) return false;
        if (salt.isEmpty() || salt.getItem() != SkofcraftItems.SALT.get()) return false;

        if (output.isEmpty()) return true;
        return output.getCount() < 64;
    }

    private void finishProcessing() {
        ItemStack dust = inputSlots.getItem(0);
        ItemStack flavor = inputSlots.getItem(1);
        ItemStack salt = inputSlots.getItem(2);
        ItemStack output = outputSlots.getItem(0);

        if (dust.isEmpty() || flavor.isEmpty() || salt.isEmpty()) return;

        ItemStack result = new ItemStack(SkofcraftItems.NEUTRAL_BASE.get());

        if (output.isEmpty()) {
            outputSlots.setItem(0, result);
        } else if (output.getCount() < 64) {
            output.grow(1);
        }

        dust.shrink(1);
        flavor.shrink(1);
        salt.shrink(1);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        int savedEnergy = tag.getInt("Energy");
        energy.extractEnergy(energy.getMaxEnergyStored(), false);
        energy.receiveEnergy(savedEnergy, false);
        processingProgress = tag.getInt("Progress");
        isProcessing = tag.getBoolean("Processing");
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("Progress", processingProgress);
        tag.putBoolean("Processing", isProcessing);
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
        return super.getCapability(cap, side);
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
        return PROCESSING_TIME;
    }

    public boolean isProcessing() {
        return isProcessing;
    }
}
