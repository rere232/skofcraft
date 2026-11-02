package ch.rere232.skofcraft.entity;

import net.minecraft.server.level.ServerPlayer;

/** A vehicle the controlling passenger flies and shoots; the client uses this for the HUD and input. */
public interface PilotedAircraft extends FueledVehicle {
    int WEAPON_GUN = 0;
    int WEAPON_MISSILE = 1;
    /** Weapons stay safe this long after a pilot climbs in, so the right-click used to board never fires a missile. */
    int BOARDING_LOCKOUT_TICKS = 40;

    /** Called on the server for the pilot's fire packets. */
    void fire(ServerPlayer pilot, int weapon);

    /** Reload time of the secondary weapon in ticks, for the HUD bar. */
    int secondaryCooldownTicks();

    float getHealth();

    float getMaxHealth();
}
