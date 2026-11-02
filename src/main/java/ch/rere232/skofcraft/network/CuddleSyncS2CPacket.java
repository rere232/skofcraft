package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.client.CuddleClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Tells everyone in range that two players started or stopped cuddling. The first one asked, the second one accepted. */
public class CuddleSyncS2CPacket {
    private final int firstId;
    private final int secondId;
    private final boolean active;

    public CuddleSyncS2CPacket(int firstId, int secondId, boolean active) {
        this.firstId = firstId;
        this.secondId = secondId;
        this.active = active;
    }

    public static void encode(CuddleSyncS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.firstId);
        buf.writeVarInt(packet.secondId);
        buf.writeBoolean(packet.active);
    }

    public static CuddleSyncS2CPacket decode(FriendlyByteBuf buf) {
        return new CuddleSyncS2CPacket(buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(CuddleSyncS2CPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> CuddleClient.sync(packet.firstId, packet.secondId, packet.active)));
        context.setPacketHandled(true);
    }
}
