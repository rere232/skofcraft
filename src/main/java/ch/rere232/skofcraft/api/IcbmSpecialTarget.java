package ch.rere232.skofcraft.api;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Something a Giga ICBM can be aimed at other than a column of the world. Addons register one with
 * {@link IcbmSpecialTargets}; the designator marks it when nothing solid is in the way.
 */
public interface IcbmSpecialTarget {
    /** Server side: is this player pointing the designator at this target right now? */
    boolean isAimedAt(Player player);

    /** Shown to the player when they mark it. */
    Component markedMessage();

    /** The launch pad's status line, given the name of the loaded payload. */
    Component padStatus(Component payload);

    /** The missile has left the atmosphere and is gone: from here on it's the target's business. */
    void onMissileArrived(ServerLevel level, UUID owner);
}
