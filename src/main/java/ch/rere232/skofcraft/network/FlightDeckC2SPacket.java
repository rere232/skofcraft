package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.entity.F14Tomcat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The F-14 is simulated by its pilot's client, so the client reports catapult launches and arrested landings. */
public class FlightDeckC2SPacket {
    public static final int CATAPULT = 0;
    public static final int TRAP = 1;

    private final int event;

    public FlightDeckC2SPacket(int event) {
        this.event = event;
    }

    public static void encode(FlightDeckC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.event);
    }

    public static FlightDeckC2SPacket decode(FriendlyByteBuf buf) {
        return new FlightDeckC2SPacket(buf.readVarInt());
    }

    public static void handle(FlightDeckC2SPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.getVehicle() instanceof F14Tomcat jet) || jet.getControllingPassenger() != player) {
                return;
            }
            SkofcraftAdvancements.award(player, packet.event == CATAPULT ? SkofcraftAdvancements.CAT_SHOT : SkofcraftAdvancements.TRAP);
        });
        context.setPacketHandled(true);
    }
}
