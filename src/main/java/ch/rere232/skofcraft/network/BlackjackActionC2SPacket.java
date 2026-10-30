package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.casino.CasinoService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BlackjackActionC2SPacket {
    private final int action;
    private final int bet;

    public BlackjackActionC2SPacket(int action, int bet) {
        this.action = action;
        this.bet = bet;
    }

    public static void encode(BlackjackActionC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.action);
        buf.writeVarInt(packet.bet);
    }

    public static BlackjackActionC2SPacket decode(FriendlyByteBuf buf) {
        return new BlackjackActionC2SPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(BlackjackActionC2SPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                CasinoService.blackjack(player, packet.action, packet.bet);
            }
        });
        context.setPacketHandled(true);
    }
}
