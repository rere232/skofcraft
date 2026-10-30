package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.casino.CasinoService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CasinoSpinC2SPacket {
    private final int bet;

    public CasinoSpinC2SPacket(int bet) {
        this.bet = bet;
    }

    public static void encode(CasinoSpinC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.bet);
    }

    public static CasinoSpinC2SPacket decode(FriendlyByteBuf buf) {
        return new CasinoSpinC2SPacket(buf.readVarInt());
    }

    public static void handle(CasinoSpinC2SPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                CasinoService.spin(player, packet.bet);
            }
        });
        context.setPacketHandled(true);
    }
}
