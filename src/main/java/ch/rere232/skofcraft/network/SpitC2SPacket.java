package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.gums.GumSlotData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SpitC2SPacket {
    public static void encode(SpitC2SPacket packet, FriendlyByteBuf buf) {
    }

    public static SpitC2SPacket decode(FriendlyByteBuf buf) {
        return new SpitC2SPacket();
    }

    public static void handle(SpitC2SPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                GumSlotData.trySpit(player);
            }
        });
        context.setPacketHandled(true);
    }
}
