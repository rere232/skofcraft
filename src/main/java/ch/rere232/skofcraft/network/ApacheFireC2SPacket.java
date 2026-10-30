package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.entity.PilotedAircraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ApacheFireC2SPacket {
    private final int weapon;

    public ApacheFireC2SPacket(int weapon) {
        this.weapon = weapon;
    }

    public static void encode(ApacheFireC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.weapon);
    }

    public static ApacheFireC2SPacket decode(FriendlyByteBuf buf) {
        return new ApacheFireC2SPacket(buf.readVarInt());
    }

    public static void handle(ApacheFireC2SPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.getVehicle() instanceof PilotedAircraft aircraft) {
                aircraft.fire(player, packet.weapon);
            }
        });
        context.setPacketHandled(true);
    }
}
