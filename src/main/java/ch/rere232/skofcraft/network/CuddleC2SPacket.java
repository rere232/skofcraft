package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.cuddle.CuddleManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The cuddle key was pressed, with the id of the entity under the crosshair (-1 for none). The server works out the rest. */
public class CuddleC2SPacket {
    private final int targetId;

    public CuddleC2SPacket(int targetId) {
        this.targetId = targetId;
    }

    public static void encode(CuddleC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.targetId);
    }

    public static CuddleC2SPacket decode(FriendlyByteBuf buf) {
        return new CuddleC2SPacket(buf.readVarInt());
    }

    public static void handle(CuddleC2SPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                CuddleManager.onKey(player, packet.targetId);
            }
        });
        context.setPacketHandled(true);
    }
}
