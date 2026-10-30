package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.screen.CasinoApp;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RouletteResultS2CPacket {
    private final int number;
    private final int bet;
    private final int payout;

    public RouletteResultS2CPacket(int number, int bet, int payout) {
        this.number = number;
        this.bet = bet;
        this.payout = payout;
    }

    public static void encode(RouletteResultS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.number);
        buf.writeVarInt(packet.bet);
        buf.writeVarInt(packet.payout);
    }

    public static RouletteResultS2CPacket decode(FriendlyByteBuf buf) {
        return new RouletteResultS2CPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(RouletteResultS2CPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> CasinoApp.onRoulette(packet.number, packet.bet, packet.payout)));
        context.setPacketHandled(true);
    }
}
