package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.phone.PhoneAddiction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PhoneUseC2SPacket {
    private final int app;

    public PhoneUseC2SPacket(int app) {
        this.app = app;
    }

    public static void encode(PhoneUseC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.app);
    }

    public static PhoneUseC2SPacket decode(FriendlyByteBuf buf) {
        return new PhoneUseC2SPacket(buf.readVarInt());
    }

    public static void handle(PhoneUseC2SPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                PhoneAddiction.onUse(player, packet.app);
            }
        });
        context.setPacketHandled(true);
    }
}
