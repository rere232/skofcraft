package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.client.ScreenFlipClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Turns the receiving player's view upside down (or back) after they flipped an Evil Lever. */
public class ScreenFlipS2CPacket {
    private final boolean flipped;

    public ScreenFlipS2CPacket(boolean flipped) {
        this.flipped = flipped;
    }

    public static void encode(ScreenFlipS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.flipped);
    }

    public static ScreenFlipS2CPacket decode(FriendlyByteBuf buf) {
        return new ScreenFlipS2CPacket(buf.readBoolean());
    }

    public static void handle(ScreenFlipS2CPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ScreenFlipClient.set(packet.flipped)));
        context.setPacketHandled(true);
    }
}
