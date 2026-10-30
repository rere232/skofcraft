package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.client.PhoneClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PhoneAddictionS2CPacket {
    private final int level;

    public PhoneAddictionS2CPacket(int level) {
        this.level = level;
    }

    public static void encode(PhoneAddictionS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.level);
    }

    public static PhoneAddictionS2CPacket decode(FriendlyByteBuf buf) {
        return new PhoneAddictionS2CPacket(buf.readVarInt());
    }

    public static void handle(PhoneAddictionS2CPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> PhoneClient.setAddiction(packet.level)));
        context.setPacketHandled(true);
    }
}
