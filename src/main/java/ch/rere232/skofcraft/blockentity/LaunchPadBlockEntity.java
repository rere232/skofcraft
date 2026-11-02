package ch.rere232.skofcraft.blockentity;

import ch.rere232.skofcraft.api.IcbmSpecialTarget;
import ch.rere232.skofcraft.api.IcbmSpecialTargets;
import ch.rere232.skofcraft.block.LaunchPadBlock;
import ch.rere232.skofcraft.entity.GigaIcbm;
import ch.rere232.skofcraft.entity.LaunchRocket;
import ch.rere232.skofcraft.item.GigaIcbmItem;
import ch.rere232.skofcraft.item.SatelliteItem;
import ch.rere232.skofcraft.space.IcbmTargets;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A launch pad's payload (a satellite or a Giga ICBM, and who loaded it), the jet fuel pumped into the rocket and,
 * for an ICBM, the column it is programmed to hit.
 */
public class LaunchPadBlockEntity extends BlockEntity {
    public static final int FUEL_NEEDED = 8;
    public static final int ICBM_FUEL_NEEDED = 32;

    private ItemStack satellite = ItemStack.EMPTY;
    @Nullable
    private UUID loader;
    private int fuel;
    @Nullable
    private BlockPos target;
    @Nullable
    private ResourceLocation specialTarget;

    public LaunchPadBlockEntity(BlockPos pos, BlockState state) {
        super(SkofcraftBlockEntities.LAUNCH_PAD.get(), pos, state);
    }

    public ItemStack getSatellite() {
        return satellite;
    }

    public boolean hasSatellite() {
        return !satellite.isEmpty();
    }

    public static boolean isPayload(ItemStack stack) {
        return stack.getItem() instanceof SatelliteItem || stack.getItem() instanceof GigaIcbmItem;
    }

    public boolean hasIcbm() {
        return satellite.getItem() instanceof GigaIcbmItem;
    }

    public int fuelNeeded() {
        return hasIcbm() ? ICBM_FUEL_NEEDED : FUEL_NEEDED;
    }

    @Nullable
    public BlockPos getTarget() {
        return target;
    }

    /** Null if the missile isn't aimed at one, or the addon that added it is gone. */
    @Nullable
    public IcbmSpecialTarget getSpecialTarget() {
        return IcbmSpecialTargets.get(specialTarget);
    }

    /** Copies the player's current mark into the missile. */
    public void programFrom(Player player) {
        specialTarget = IcbmTargets.getSpecial(player);
        target = specialTarget != null ? null : IcbmTargets.get(player);
        changed();
    }

    public int getFuel() {
        return fuel;
    }

    public boolean isReady() {
        return hasSatellite() && fuel >= fuelNeeded() && (!hasIcbm() || target != null || getSpecialTarget() != null);
    }

    public void loadSatellite(ItemStack stack, UUID player) {
        satellite = stack.copyWithCount(1);
        loader = player;
        changed();
    }

    public ItemStack unload() {
        ItemStack taken = satellite;
        satellite = ItemStack.EMPTY;
        loader = null;
        target = null;
        specialTarget = null;
        changed();
        return taken;
    }

    public boolean addFuel() {
        if (fuel >= (hasSatellite() ? fuelNeeded() : ICBM_FUEL_NEEDED)) {
            return false;
        }
        fuel++;
        changed();
        return true;
    }

    /** Puts the rocket on the pad and starts the countdown; the satellite and fuel go with it. */
    public boolean launch(ServerLevel level, UUID fallbackOwner) {
        if (!isReady()) {
            return false;
        }
        UUID owner = loader != null ? loader : fallbackOwner;
        int burned = fuelNeeded();
        if (satellite.getItem() instanceof SatelliteItem item) {
            level.addFreshEntity(LaunchRocket.create(level, worldPosition, LaunchPadBlock.TOP, item.type, owner));
        } else if (satellite.getItem() instanceof GigaIcbmItem icbm && (target != null || getSpecialTarget() != null)) {
            level.addFreshEntity(GigaIcbm.create(level, worldPosition, LaunchPadBlock.TOP, target, specialTarget, owner, icbm.yieldScale));
        } else {
            return false;
        }
        satellite = ItemStack.EMPTY;
        loader = null;
        target = null;
        specialTarget = null;
        // A satellite launch burns only its share; the rest stays in the tanks
        fuel -= burned;
        changed();
        return true;
    }

    /** Tall enough to keep drawing the rocket standing on the pad when only its upper part is on screen. */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).expandTowards(0.0D, 10.0D, 0.0D).inflate(1.0D, 0.0D, 1.0D);
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        if (!satellite.isEmpty()) {
            tag.put("Satellite", satellite.save(new CompoundTag()));
        }
        if (loader != null) {
            tag.putUUID("Loader", loader);
        }
        tag.putInt("Fuel", fuel);
        if (target != null) {
            tag.putInt("TargetX", target.getX());
            tag.putInt("TargetZ", target.getZ());
        }
        if (specialTarget != null) {
            tag.putString("SpecialTarget", specialTarget.toString());
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        satellite = tag.contains("Satellite") ? ItemStack.of(tag.getCompound("Satellite")) : ItemStack.EMPTY;
        loader = tag.hasUUID("Loader") ? tag.getUUID("Loader") : null;
        fuel = tag.getInt("Fuel");
        target = tag.contains("TargetX") ? new BlockPos(tag.getInt("TargetX"), 0, tag.getInt("TargetZ")) : null;
        specialTarget = tag.contains("SpecialTarget") ? ResourceLocation.tryParse(tag.getString("SpecialTarget")) : null;
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        if (!satellite.isEmpty()) {
            tag.put("Satellite", satellite.save(new CompoundTag()));
        }
        tag.putInt("Fuel", fuel);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
