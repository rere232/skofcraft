package ch.rere232.skofcraft.blockentity;

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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FEGrinderBlockEntity extends BlockEntity {
    private static final int CAPACITY = 10000;
    private static final int MAX_RECEIVE = 100;
    private static final int PROCESSING_TIME = 150;
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

    private final Container inputSlots = new SimpleContainer(1);
    private final Container outputSlots = new SimpleContainer(1);
    private final LazyOptional<IItemHandler> inputItemHandler = LazyOptional.of(() -> new InvWrapper(inputSlots));
    private final LazyOptional<IItemHandler> outputItemHandler = LazyOptional.of(() -> new InvWrapper(outputSlots));
    private final LazyOptional<IItemHandler> combinedItemHandler = LazyOptional.of(() -> new CombinedInvWrapper(new InvWrapper(inputSlots), new InvWrapper(outputSlots)));

    private int processingProgress = 0;
    private boolean isProcessing = false;
    private int manualWorkTicks = 0;

    public FEGrinderBlockEntity(BlockPos blockPos, BlockState blockState) {
        this(blockPos, blockState, true);
    }

    public FEGrinderBlockEntity(BlockPos blockPos, BlockState blockState, boolean requiresEnergy) {
        super(SkofcraftBlockEntities.FE_GRINDER.get(), blockPos, blockState);
        this.requiresEnergy = requiresEnergy && !isManualBlock(blockState.getBlock());
    }

    private static boolean isManualBlock(Block block) {
        return block == SkofcraftBlocks.MANUAL_GRINDER.get();
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

            if (processingProgress >= PROCESSING_TIME) {
                finishProcessing();
                isProcessing = false;
                processingProgress = 0;
            }
            setChanged();
        }
    }

    private boolean canProcess() {
        ItemStack input = inputSlots.getItem(0);
        ItemStack output = outputSlots.getItem(0);

        if (input.isEmpty() || input.getItem() != SkofcraftItems.TOBACCO_LEAF_DRY.get()) {
            return false;
        }

        if (output.isEmpty()) {
            return true;
        }

        return output.getItem() == SkofcraftItems.TOBACCO_DUST.get() && output.getCount() < 64;
    }

    private void finishProcessing() {
        ItemStack input = inputSlots.getItem(0);
        ItemStack output = outputSlots.getItem(0);

        if (input.isEmpty() || input.getItem() != SkofcraftItems.TOBACCO_LEAF_DRY.get()) return;

        ItemStack result = new ItemStack(SkofcraftItems.TOBACCO_DUST.get(), 2);

        if (output.isEmpty()) {
            outputSlots.setItem(0, result);
        } else if (output.getItem() == result.getItem() && output.getCount() < 64) {
            output.grow(Math.min(2, 64 - output.getCount()));
        }

        input.shrink(1);
    }

    public boolean manualCrank() {
        if (requiresEnergy || !canProcess()) {
            return false;
        }
        if (!isProcessing) {
            isProcessing = true;
            processingProgress = 0;
        }
        manualWorkTicks = Math.min(manualWorkTicks + 20, PROCESSING_TIME);
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
            if (side == null) {
                return combinedItemHandler.cast();
            }
            return (side == Direction.DOWN ? outputItemHandler : inputItemHandler).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyHandler.invalidate();
        inputItemHandler.invalidate();
        outputItemHandler.invalidate();
        combinedItemHandler.invalidate();
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
        return PROCESSING_TIME;
    }

    public boolean isProcessing() {
        return isProcessing;
    }
}
