package ch.rere232.skofcraft.space;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Each player's Giga ICBM target, marked with the Orbital Strike Designator or on the phone's Sat Map. It is copied
 * into a launch pad when an ICBM is loaded (or the pad is reprogrammed with a designator).
 */
public final class IcbmTargets {
    private static final String ROOT = "SkofcraftIcbmTarget";

    private IcbmTargets() {}

    public static void set(Player player, int x, int z) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", player.level().dimension().location().toString());
        tag.putInt("X", x);
        tag.putInt("Z", z);
        player.getPersistentData().put(ROOT, tag);
    }

    /** The marked column in the player's current dimension, with y = 0; null if none was marked here. */
    @Nullable
    public static BlockPos get(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT)) {
            return null;
        }
        CompoundTag tag = persistent.getCompound(ROOT);
        if (!tag.getString("Dimension").equals(player.level().dimension().location().toString())) {
            return null;
        }
        return new BlockPos(tag.getInt("X"), 0, tag.getInt("Z"));
    }
}
