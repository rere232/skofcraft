package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.gums.GumSlotData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GumRemoveC2SPacket {
    private final int slot;

    public GumRemoveC2SPacket(int slot) {
        this.slot = slot;
    }

    public static void encode(GumRemoveC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.slot);
    }

    public static GumRemoveC2SPacket decode(FriendlyByteBuf buf) {
        return new GumRemoveC2SPacket(buf.readVarInt());
    }

    public static void handle(GumRemoveC2SPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && packet.slot >= 0 && packet.slot < GumSlotData.SLOT_COUNT) {
                GumSlotData.get(player).removeSlot(packet.slot);
            }
        });
        context.setPacketHandled(true);
    }
}
