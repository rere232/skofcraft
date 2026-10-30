package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.entity.PilotedAircraft;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Client-side state shared by the aircraft HUDs and input handling. */
public final class AircraftClient {
    /** Game time of the last secondary weapon shot the client asked for, to draw the reload bar. */
    static long lastSecondaryTick = Long.MIN_VALUE / 2;

    private AircraftClient() {}

    /** The aircraft the local player is flying, or null. */
    static PilotedAircraft piloted() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return null;
        }
        Entity vehicle = mc.player.getVehicle();
        if (vehicle instanceof PilotedAircraft aircraft && vehicle.getControllingPassenger() == mc.player) {
            return aircraft;
        }
        return null;
    }
}
