package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.client.ButtonCrashClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** You pressed the button. Sent only to the player who pressed it. */
public class ButtonCrashS2CPacket {
    public static void encode(ButtonCrashS2CPacket packet, FriendlyByteBuf buf) {
    }

    public static ButtonCrashS2CPacket decode(FriendlyByteBuf buf) {
        return new ButtonCrashS2CPacket();
    }

    public static void handle(ButtonCrashS2CPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ButtonCrashClient::arm));
        context.setPacketHandled(true);
    }
}
