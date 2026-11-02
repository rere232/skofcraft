package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.entity.Postbus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PostbusHornC2SPacket {
    public static void encode(PostbusHornC2SPacket packet, FriendlyByteBuf buf) {
    }

    public static PostbusHornC2SPacket decode(FriendlyByteBuf buf) {
        return new PostbusHornC2SPacket();
    }

    public static void handle(PostbusHornC2SPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.getVehicle() instanceof Postbus bus) {
                bus.honk(player);
            }
        });
        context.setPacketHandled(true);
    }
}
