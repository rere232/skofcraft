package ch.rere232.skofcraft.space;

import java.util.Locale;

/** What a satellite does once it is in orbit. */
public enum SatelliteType {
    /** Extends the range of every powered radar in its dimension. */
    RECON,
    /** Drops tungsten rods on whatever its owner points an Orbital Strike Designator at. */
    STRIKE;

    public String key() {
        return "satellite.skofcraft." + name().toLowerCase(Locale.ROOT);
    }

    public static SatelliteType byId(int id) {
        SatelliteType[] values = values();
        return values[Math.floorMod(id, values.length)];
    }
}
