package ch.rere232.skofcraft.blockentity;

import ch.rere232.skofcraft.radar.RadarCategory;
import ch.rere232.skofcraft.radar.RadarContact;
import ch.rere232.skofcraft.radar.RadarMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Scans for entities around the block. Unpowered it sees {@link #BASE_RANGE} blocks; fed FE it sees
 * {@link #POWERED_RANGE}. It outputs a redstone signal that grows as the nearest contact in its filter closes in.
 */
public class RadarBlockEntity extends BlockEntity {
    public static final int BASE_RANGE = 64;
    public static final int POWERED_RANGE = 256;
    public static final int ENERGY_PER_TICK = 40;
    public static final int CAPACITY = 20000;
    public static final int MAX_CONTACTS = 64;
    private static final int MAX_RECEIVE = 2000;
    private static final int SCAN_INTERVAL = 20;

    private final Battery energy = new Battery();
    private final LazyOptional<EnergyStorage> energyHandler = LazyOptional.of(() -> energy);

    /** Accepts FE from outside but never gives any back; the radar spends it itself. */
    private final class Battery extends EnergyStorage {
        Battery() {
            super(CAPACITY, MAX_RECEIVE, 0, 0);
        }

        void set(int amount) {
            energy = Math.max(0, Math.min(capacity, amount));
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
            }
            return received;
        }
    }

    private RadarMode mode = RadarMode.ALL;
    private boolean powered;
    private int signal;
    private int ticks;

    public RadarBlockEntity(BlockPos pos, BlockState state) {
        super(SkofcraftBlockEntities.RADAR.get(), pos, state);
    }

    public void tick() {
        if (level == null) {
            return;
        }
        powered = energy.getEnergyStored() >= ENERGY_PER_TICK;
        if (powered) {
            energy.set(energy.getEnergyStored() - ENERGY_PER_TICK);
        }
        if (++ticks % SCAN_INTERVAL == 0) {
            List<RadarContact> contacts = scan(null);
            int newSignal = contacts.isEmpty() ? 0
                    : Math.max(1, 15 - (int) (14.0D * contacts.get(0).horizontalDistance() / range()));
            if (newSignal != signal) {
                signal = newSignal;
                setChanged();
                level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            }
        }
    }

    /** Contacts in range that the current filter shows, nearest first, leaving out {@code viewer}. */
    public List<RadarContact> scan(@Nullable Entity viewer) {
        List<RadarContact> contacts = new ArrayList<>();
        if (level == null) {
            return contacts;
        }
        int range = range();
        Vec3 center = Vec3.atCenterOf(worldPosition);
        AABB box = new AABB(worldPosition).inflate(range, range, range);
        for (Entity entity : level.getEntities((Entity) null, box, e -> e != viewer)) {
            RadarCategory category = RadarCategory.of(entity);
            if (category == null || !mode.shows(category)) {
                continue;
            }
            float dx = (float) (entity.getX() - center.x);
            float dz = (float) (entity.getZ() - center.z);
            if (dx * dx + dz * dz > (float) range * range) {
                continue;
            }
            contacts.add(new RadarContact(category, dx, (float) (entity.getY() - center.y), dz, entity.getName()));
        }
        contacts.sort(Comparator.comparingDouble(RadarContact::horizontalDistance));
        return contacts.size() > MAX_CONTACTS ? new ArrayList<>(contacts.subList(0, MAX_CONTACTS)) : contacts;
    }

    public int range() {
        return powered ? POWERED_RANGE : BASE_RANGE;
    }

    public boolean isPowered() {
        return powered;
    }

    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    public int getSignal() {
        return signal;
    }

    public RadarMode getMode() {
        return mode;
    }

    public void setMode(RadarMode mode) {
        if (this.mode != mode) {
            this.mode = mode;
            ticks = SCAN_INTERVAL - 1; // rescan on the next tick so the redstone output follows the new filter
            setChanged();
        }
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("Mode", mode.ordinal());
        tag.putInt("Signal", signal);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        energy.set(tag.getInt("Energy"));
        mode = RadarMode.byId(tag.getInt("Mode"));
        signal = tag.getInt("Signal");
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyHandler.invalidate();
    }
}
