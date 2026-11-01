package ch.rere232.skofcraft.radar;

/** Radar filter: which contacts appear on the scope and trigger the redstone output. */
public enum RadarMode {
    ALL,
    AIR_SEA,
    PLAYERS,
    HOSTILES;

    public boolean shows(RadarCategory category) {
        return switch (this) {
            case ALL -> true;
            case AIR_SEA -> category == RadarCategory.AIRCRAFT || category == RadarCategory.SHIP;
            case PLAYERS -> category == RadarCategory.PLAYER;
            case HOSTILES -> category == RadarCategory.HOSTILE;
        };
    }

    public String key() {
        return "screen.skofcraft.radar.mode." + name().toLowerCase(java.util.Locale.ROOT);
    }

    public RadarMode next() {
        return byId(ordinal() + 1);
    }

    public static RadarMode byId(int id) {
        RadarMode[] values = values();
        return values[Math.floorMod(id, values.length)];
    }
}
