package ch.rere232.skofcraft.space;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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

    /** Marks a special target, which replaces any column marked before. */
    public static void setSpecial(Player player, ResourceLocation id) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", player.level().dimension().location().toString());
        tag.putString("Special", id.toString());
        player.getPersistentData().put(ROOT, tag);
    }

    /** The special target marked in the player's current dimension; null if there is none. */
    @Nullable
    public static ResourceLocation getSpecial(Player player) {
        CompoundTag tag = player.getPersistentData().getCompound(ROOT);
        if (!tag.contains("Special") || !tag.getString("Dimension").equals(player.level().dimension().location().toString())) {
            return null;
        }
        return ResourceLocation.tryParse(tag.getString("Special"));
    }

    /** The marked column in the player's current dimension, with y = 0; null if none was marked here. */
    @Nullable
    public static BlockPos get(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT)) {
            return null;
        }
        CompoundTag tag = persistent.getCompound(ROOT);
        if (tag.contains("Special") || !tag.getString("Dimension").equals(player.level().dimension().location().toString())) {
            return null;
        }
        return new BlockPos(tag.getInt("X"), 0, tag.getInt("Z"));
    }
}
