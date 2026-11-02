package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.entity.FpvDrone;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The pilot's client reporting where its drone is and what just happened to it, once a tick. */
public class FpvDroneStateC2SPacket {
    private final int droneId;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final int flags;

    public FpvDroneStateC2SPacket(int droneId, double x, double y, double z, float yaw, float pitch, int flags) {
        this.droneId = droneId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.flags = flags;
    }

    public static void encode(FpvDroneStateC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.droneId);
        buf.writeDouble(packet.x);
        buf.writeDouble(packet.y);
        buf.writeDouble(packet.z);
        buf.writeFloat(packet.yaw);
        buf.writeFloat(packet.pitch);
        buf.writeByte(packet.flags);
    }

    public static FpvDroneStateC2SPacket decode(FriendlyByteBuf buf) {
        return new FpvDroneStateC2SPacket(buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readFloat(), buf.readByte());
    }

    public static void handle(FpvDroneStateC2SPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.level().getEntity(packet.droneId) instanceof FpvDrone drone) {
                drone.pilotUpdate(player, packet.x, packet.y, packet.z, packet.yaw, packet.pitch, packet.flags);
            }
        });
        context.setPacketHandled(true);
    }
}
