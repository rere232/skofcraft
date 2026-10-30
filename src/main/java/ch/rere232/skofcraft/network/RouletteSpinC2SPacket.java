package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.casino.CasinoService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RouletteSpinC2SPacket {
    private final int bet;
    private final int type;
    private final int pick;

    public RouletteSpinC2SPacket(int bet, int type, int pick) {
        this.bet = bet;
        this.type = type;
        this.pick = pick;
    }

    public static void encode(RouletteSpinC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.bet);
        buf.writeVarInt(packet.type);
        buf.writeVarInt(packet.pick);
    }

    public static RouletteSpinC2SPacket decode(FriendlyByteBuf buf) {
        return new RouletteSpinC2SPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(RouletteSpinC2SPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                CasinoService.roulette(player, packet.bet, packet.type, packet.pick);
            }
        });
        context.setPacketHandled(true);
    }
}
