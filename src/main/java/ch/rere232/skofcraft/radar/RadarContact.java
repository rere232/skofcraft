package ch.rere232.skofcraft.radar;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

/** One blip: an entity's offset from the radar (x east, z south, y up) and its name. */
public record RadarContact(RadarCategory category, float dx, float dy, float dz, Component name) {
    public double horizontalDistance() {
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Compass bearing from the radar in degrees: 0 north, 90 east. */
    public float bearing() {
        float degrees = (float) Math.toDegrees(Math.atan2(dx, -dz));
        return degrees < 0.0F ? degrees + 360.0F : degrees;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeByte(category.ordinal());
        buf.writeFloat(dx);
        buf.writeFloat(dy);
        buf.writeFloat(dz);
        buf.writeComponent(name);
    }

    public static RadarContact read(FriendlyByteBuf buf) {
        return new RadarContact(RadarCategory.byId(buf.readByte()), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readComponent());
    }
}
