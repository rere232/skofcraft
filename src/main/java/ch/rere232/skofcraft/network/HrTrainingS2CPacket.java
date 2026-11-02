package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.client.HrClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Starts the receiving player's training screen, or lets them out of it. */
public class HrTrainingS2CPacket {
    private final boolean active;

    public HrTrainingS2CPacket(boolean active) {
        this.active = active;
    }

    public static void encode(HrTrainingS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.active);
    }

    public static HrTrainingS2CPacket decode(FriendlyByteBuf buf) {
        return new HrTrainingS2CPacket(buf.readBoolean());
    }

    public static void handle(HrTrainingS2CPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> HrClient.set(packet.active)));
        context.setPacketHandled(true);
    }
}
