package ch.rere232.skofcraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Holds the water and ground SKOFee doses of a SKOFee Machine; each cup uses one of each. */
public class SkofeeMachineBlockEntity extends BlockEntity {
    public static final int MAX_WATER = 16;
    public static final int MAX_DOSES = 32;
    public static final int WATER_PER_BUCKET = 8;

    private int water = 0;
    private int doses = 0;

    public SkofeeMachineBlockEntity(BlockPos pos, BlockState state) {
        super(SkofcraftBlockEntities.SKOFEE_MACHINE.get(), pos, state);
    }

    /** Returns false if the tank cannot take a whole bucket. */
    public boolean addBucket() {
        if (water + WATER_PER_BUCKET > MAX_WATER) {
            return false;
        }
        water += WATER_PER_BUCKET;
        sync();
        return true;
    }

    /** Adds up to {@code count} doses and returns how many fit. */
    public int addDoses(int count) {
        int added = Math.min(count, MAX_DOSES - doses);
        if (added > 0) {
            doses += added;
            sync();
        }
        return added;
    }

    public boolean canBrew() {
        return water > 0 && doses > 0;
    }

    public void brew() {
        water--;
        doses--;
        sync();
    }

    public int getWater() {
        return water;
    }

    public int getDoses() {
        return doses;
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        water = tag.getInt("Water");
        doses = tag.getInt("Doses");
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Water", water);
        tag.putInt("Doses", doses);
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
}
