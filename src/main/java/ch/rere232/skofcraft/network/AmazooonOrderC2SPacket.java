package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.delivery.DeliveryService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AmazooonOrderC2SPacket {
    private final int product;

    public AmazooonOrderC2SPacket(int product) {
        this.product = product;
    }

    public static void encode(AmazooonOrderC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.product);
    }

    public static AmazooonOrderC2SPacket decode(FriendlyByteBuf buf) {
        return new AmazooonOrderC2SPacket(buf.readVarInt());
    }

    public static void handle(AmazooonOrderC2SPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                DeliveryService.order(player, packet.product);
            }
        });
        context.setPacketHandled(true);
    }
}
