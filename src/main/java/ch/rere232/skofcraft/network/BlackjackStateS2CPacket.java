package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.screen.CasinoApp;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BlackjackStateS2CPacket {
    private final int phase;
    private final int result;
    private final int bet;
    private final int payout;
    private final int[] player;
    private final int[] dealer;

    public BlackjackStateS2CPacket(int phase, int result, int bet, int payout, int[] player, int[] dealer) {
        this.phase = phase;
        this.result = result;
        this.bet = bet;
        this.payout = payout;
        this.player = player;
        this.dealer = dealer;
    }

    public static void encode(BlackjackStateS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.phase);
        buf.writeVarInt(packet.result);
        buf.writeVarInt(packet.bet);
        buf.writeVarInt(packet.payout);
        buf.writeVarIntArray(packet.player);
        buf.writeVarIntArray(packet.dealer);
    }

    public static BlackjackStateS2CPacket decode(FriendlyByteBuf buf) {
        return new BlackjackStateS2CPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarIntArray(16), buf.readVarIntArray(16));
    }

    public static void handle(BlackjackStateS2CPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> CasinoApp.onBlackjack(packet.phase, packet.result, packet.bet, packet.payout,
                        packet.player, packet.dealer)));
        context.setPacketHandled(true);
    }
}
