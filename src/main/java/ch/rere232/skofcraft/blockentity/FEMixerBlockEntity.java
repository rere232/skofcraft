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

public class FEMixerBlockEntity extends BlockEntity {
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
    private final LazyOptional<MachineItemHandler> itemHandler = LazyOptional.of(() -> new MachineItemHandler(inputSlots, outputSlots, this::setChanged, (slot, stack) -> switch (slot) {
        case 0 -> stack.is(SkofcraftItems.NEUTRAL_BASE.get()) || stack.is(SkofcraftItems.TOBACCO_DUST.get()) || stack.is(SkofcraftItems.EMPTY_VAPE.get());
        case 1 -> isNicotineSource(stack.getItem()) || stack.is(SkofcraftItems.NICOTINE_MIX.get());
        case 2 -> isFlavorItem(stack.getItem());
        default -> false;
    }));

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
        this.energyCost = industrial ? 200 : 60;
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

    /** Vape, snuff or nicotine mix for the current inputs; empty if the inputs are invalid. */
    private ItemStack buildResult() {
        ItemStack base = inputSlots.getItem(0);
        ItemStack nicotine = inputSlots.getItem(1);
        ItemStack flavor = inputSlots.getItem(2);

        if (base.isEmpty()) return ItemStack.EMPTY;

        if (base.getItem() == SkofcraftItems.EMPTY_VAPE.get()) {
            if (!flavor.isEmpty() || nicotine.getItem() != SkofcraftItems.NICOTINE_MIX.get()) return ItemStack.EMPTY;
            ItemStack vape = new ItemStack(SkofcraftItems.VAPE.get());
            GumConsumableItem.copyTraits(nicotine, vape);
            return vape;
        }

        if (flavor.isEmpty() || !isFlavorItem(flavor.getItem())) return ItemStack.EMPTY;

        if (base.getItem() == SkofcraftItems.TOBACCO_DUST.get()) {
            if (!nicotine.isEmpty()) return ItemStack.EMPTY;
            ItemStack snuff = new ItemStack(SkofcraftItems.SNUFF.get());
            GumConsumableItem.setFlavor(snuff, flavor);
            return snuff;
        }

        if (base.getItem() != SkofcraftItems.NEUTRAL_BASE.get()) return ItemStack.EMPTY;
        if (nicotine.isEmpty() || !isNicotineSource(nicotine.getItem())) return ItemStack.EMPTY;

        ItemStack result = new ItemStack(SkofcraftItems.NICOTINE_MIX.get());
        GumConsumableItem.setFlavor(result, flavor);
        GumConsumableItem.setStrength(result, nicotineStrength(nicotine));
        return result;
    }

    /** Extract gives a normal mix, one crystal a strong one, two or more crystals an extra strong one. */
    private static int nicotineStrength(ItemStack nicotine) {
        if (nicotine.getItem() == SkofcraftItems.NICOTINE_CRYSTALS.get()) {
            return nicotine.getCount() >= 2 ? 2 : 1;
        }
        return 0;
    }

    private static boolean isNicotineSource(Object item) {
        return item == SkofcraftItems.NICOTINE_EXTRACT.get() || item == SkofcraftItems.NICOTINE_CRYSTALS.get();
    }

    private void finishProcessing() {
        ItemStack base = inputSlots.getItem(0);
        ItemStack nicotine = inputSlots.getItem(1);
        ItemStack flavor = inputSlots.getItem(2);
        ItemStack output = outputSlots.getItem(0);

        ItemStack result = buildResult();
        if (result.isEmpty()) return;
        int nicotineCost = nicotineStrength(nicotine) == 2 ? 2 : 1;

        if (output.isEmpty()) {
            outputSlots.setItem(0, result);
        } else if (ItemStack.isSameItemSameTags(output, result) && output.getCount() < 64) {
            output.grow(1);
        }

        base.shrink(1);
        if (!nicotine.isEmpty()) {
            nicotine.shrink(nicotineCost);
        }
        if (!flavor.isEmpty()) {
            flavor.shrink(1);
        }
    }

    private boolean isFlavorItem(Object item) {
        return item == SkofcraftItems.FLAVOR_MINT.get()
                || item == SkofcraftItems.FLAVOR_ICE_MINT.get()
                || item == SkofcraftItems.FLAVOR_BERRY.get()
                || item == SkofcraftItems.FLAVOR_CITRUS.get()
                || item == SkofcraftItems.FLAVOR_LICORICE.get()
                || item == SkofcraftItems.FLAVOR_COLA.get()
                || item == SkofcraftItems.FLAVOR_COFFEE.get()
                || item == SkofcraftItems.FLAVOR_NATURE.get()
                || item == SkofcraftItems.FLAVOR_REGENTS_BLEND.get()
                || item == SkofcraftItems.FLAVOR_ROYAL_RESERVE.get();
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
        tag.putInt("ManualWorkTicks", manualWorkTicks);

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
