package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.hr.MandatoryHr;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The player acknowledged the last page of their training. */
public class HrCompleteC2SPacket {
    public static void encode(HrCompleteC2SPacket packet, FriendlyByteBuf buf) {
    }

    public static HrCompleteC2SPacket decode(FriendlyByteBuf buf) {
        return new HrCompleteC2SPacket();
    }

    public static void handle(HrCompleteC2SPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                MandatoryHr.complete(player);
            }
        });
        context.setPacketHandled(true);
    }
}
