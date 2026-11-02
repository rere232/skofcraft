package ch.rere232.skofcraft.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The special targets addons have added. A mark or a loaded missile is saved by id, so one whose addon was later
 * removed simply stops resolving here: the pad then reports no target.
 */
public final class IcbmSpecialTargets {
    /** Mods are constructed in parallel, which is when addons are expected to register. */
    private static final Map<ResourceLocation, IcbmSpecialTarget> TARGETS = new ConcurrentHashMap<>();

    private IcbmSpecialTargets() {}

    public static void register(ResourceLocation id, IcbmSpecialTarget target) {
        TARGETS.put(id, target);
    }

    @Nullable
    public static IcbmSpecialTarget get(@Nullable ResourceLocation id) {
        return id == null ? null : TARGETS.get(id);
    }

    /** The id of a target this player is aiming at, or null. */
    @Nullable
    public static ResourceLocation aimedAt(Player player) {
        for (Map.Entry<ResourceLocation, IcbmSpecialTarget> entry : TARGETS.entrySet()) {
            if (entry.getValue().isAimedAt(player)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
