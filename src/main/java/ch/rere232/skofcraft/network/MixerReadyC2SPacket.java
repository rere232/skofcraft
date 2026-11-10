package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.blockentity.DjMixerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * A listener has the mixer's song buffered and ready to play. The mixer holds its clock at the start until the first
 * of these comes in, so nobody misses the beginning of the song while YouTube is being asked for it.
 */
public class MixerReadyC2SPacket {
    /** Listeners hear a mixer from a fair way off; this covers the furthest one that streams it. */
    private static final double MAX_DISTANCE_SQR = 96.0D * 96.0D;

    private final BlockPos pos;
    private final int revision;

    public MixerReadyC2SPacket(BlockPos pos, int revision) {
        this.pos = pos;
        this.revision = revision;
    }

    public static void encode(MixerReadyC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeVarInt(packet.revision);
    }

    public static MixerReadyC2SPacket decode(FriendlyByteBuf buf) {
        return new MixerReadyC2SPacket(buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(MixerReadyC2SPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.distanceToSqr(packet.pos.getCenter()) <= MAX_DISTANCE_SQR
                    && player.level().isLoaded(packet.pos)
                    && player.level().getBlockEntity(packet.pos) instanceof DjMixerBlockEntity mixer) {
                mixer.ready(packet.revision);
            }
        });
        context.setPacketHandled(true);
    }
}
