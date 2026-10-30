package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.entity.F14Tomcat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The pilot reports that the jet hit something too hard. */
public class JetCrashC2SPacket {
    public static void encode(JetCrashC2SPacket packet, FriendlyByteBuf buf) {}

    public static JetCrashC2SPacket decode(FriendlyByteBuf buf) {
        return new JetCrashC2SPacket();
    }

    public static void handle(JetCrashC2SPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.getVehicle() instanceof F14Tomcat jet && jet.getControllingPassenger() == player) {
                jet.crash();
            }
        });
        context.setPacketHandled(true);
    }
}
