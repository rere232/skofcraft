package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.audio.MixerTrack;
import ch.rere232.skofcraft.blockentity.DjMixerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Songs picked on a DJ mixer screen, to append to its queue or to play right away. */
public class MixerAddC2SPacket {
    /** Enough for a whole playlist in one go; the queue itself stops at {@link DjMixerBlockEntity#MAX_QUEUE}. */
    public static final int MAX_TRACKS = 100;

    private final BlockPos pos;
    private final List<MixerTrack> tracks;
    private final boolean playNow;

    public MixerAddC2SPacket(BlockPos pos, List<MixerTrack> tracks, boolean playNow) {
        this.pos = pos;
        this.tracks = tracks.size() > MAX_TRACKS ? tracks.subList(0, MAX_TRACKS) : tracks;
        this.playNow = playNow;
    }

    public static void encode(MixerAddC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeBoolean(packet.playNow);
        buf.writeVarInt(packet.tracks.size());
        for (MixerTrack track : packet.tracks) {
            track.write(buf);
        }
    }

    public static MixerAddC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        boolean playNow = buf.readBoolean();
        int count = Math.min(MAX_TRACKS, buf.readVarInt());
        List<MixerTrack> tracks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            MixerTrack track = MixerTrack.read(buf);
            if (track != null) {
                tracks.add(track);
            }
        }
        return new MixerAddC2SPacket(pos, tracks, playNow);
    }

    public static void handle(MixerAddC2SPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DjMixerBlockEntity mixer = MixerActionC2SPacket.mixerFor(context.getSender(), packet.pos);
            if (mixer != null && !packet.tracks.isEmpty()) {
                mixer.addTracks(packet.tracks, packet.playNow);
            }
        });
        context.setPacketHandled(true);
    }
}
