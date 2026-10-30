package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.casino.SlotMachine;
import ch.rere232.skofcraft.screen.CasinoApp;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CasinoResultS2CPacket {
    private final int[] reels;
    private final int bet;
    private final int payout;

    public CasinoResultS2CPacket(int[] reels, int bet, int payout) {
        this.reels = reels;
        this.bet = bet;
        this.payout = payout;
    }

    public static void encode(CasinoResultS2CPacket packet, FriendlyByteBuf buf) {
        for (int reel : packet.reels) {
            buf.writeVarInt(reel);
        }
        buf.writeVarInt(packet.bet);
        buf.writeVarInt(packet.payout);
    }

    public static CasinoResultS2CPacket decode(FriendlyByteBuf buf) {
        int[] reels = new int[SlotMachine.REELS];
        for (int i = 0; i < reels.length; i++) {
            reels[i] = buf.readVarInt();
        }
        return new CasinoResultS2CPacket(reels, buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(CasinoResultS2CPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> CasinoApp.onResult(packet.reels, packet.bet, packet.payout)));
        context.setPacketHandled(true);
    }
}
