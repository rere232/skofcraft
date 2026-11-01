package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.client.NuclearBlastClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** A reactor melted down here: clients draw the flash, the shockwave and the mushroom cloud themselves. */
public class MushroomCloudS2CPacket {
    /** How far away players are told about it; the cloud is visible from this far. */
    public static final double RANGE = 512.0D;

    private final double x;
    private final double y;
    private final double z;
    /** Size relative to a reactor meltdown's cloud. */
    private final float scale;
    /** Burns purple: the Giga ICBM's dragon warhead. */
    private final boolean draconic;

    public MushroomCloudS2CPacket(double x, double y, double z) {
        this(x, y, z, 1.0F, false);
    }

    public MushroomCloudS2CPacket(double x, double y, double z, float scale, boolean draconic) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.scale = scale;
        this.draconic = draconic;
    }

    public static void encode(MushroomCloudS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeDouble(packet.x);
        buf.writeDouble(packet.y);
        buf.writeDouble(packet.z);
        buf.writeFloat(packet.scale);
        buf.writeBoolean(packet.draconic);
    }

    public static MushroomCloudS2CPacket decode(FriendlyByteBuf buf) {
        return new MushroomCloudS2CPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readBoolean());
    }

    public static void handle(MushroomCloudS2CPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> NuclearBlastClient.start(packet.x, packet.y, packet.z, packet.scale, packet.draconic)));
        context.setPacketHandled(true);
    }
}
