package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.screen.MapApp;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/** Satellite status for the phone's map app, with the outcome of a strike request when there was one. */
public class SatelliteLinkS2CPacket {
    public final int recon;
    public final int strikeTotal;
    public final int strikeReady;
    public final int secondsToNext;
    @Nullable
    public final Component message;
    public final boolean struck;
    public final int x;
    public final int z;

    public SatelliteLinkS2CPacket(int recon, int strikeTotal, int strikeReady, int secondsToNext, @Nullable Component message,
                                  boolean struck, int x, int z) {
        this.recon = recon;
        this.strikeTotal = strikeTotal;
        this.strikeReady = strikeReady;
        this.secondsToNext = secondsToNext;
        this.message = message;
        this.struck = struck;
        this.x = x;
        this.z = z;
    }

    public static void encode(SatelliteLinkS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.recon);
        buf.writeVarInt(packet.strikeTotal);
        buf.writeVarInt(packet.strikeReady);
        buf.writeVarInt(packet.secondsToNext);
        buf.writeBoolean(packet.message != null);
        if (packet.message != null) {
            buf.writeComponent(packet.message);
        }
        buf.writeBoolean(packet.struck);
        buf.writeInt(packet.x);
        buf.writeInt(packet.z);
    }

    public static SatelliteLinkS2CPacket decode(FriendlyByteBuf buf) {
        int recon = buf.readVarInt();
        int strikeTotal = buf.readVarInt();
        int strikeReady = buf.readVarInt();
        int secondsToNext = buf.readVarInt();
        Component message = buf.readBoolean() ? buf.readComponent() : null;
        return new SatelliteLinkS2CPacket(recon, strikeTotal, strikeReady, secondsToNext, message, buf.readBoolean(), buf.readInt(), buf.readInt());
    }

    public static void handle(SatelliteLinkS2CPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MapApp.receive(packet)));
        context.setPacketHandled(true);
    }
}
