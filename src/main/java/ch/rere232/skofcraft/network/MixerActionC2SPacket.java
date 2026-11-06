package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.blockentity.DjMixerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** A button pressed on a DJ mixer screen. {@code value} is the queue index, seek position or volume, depending on the action. */
public class MixerActionC2SPacket {
    public static final int PLAY_PAUSE = 0;
    public static final int STOP = 1;
    public static final int NEXT = 2;
    public static final int PREVIOUS = 3;
    public static final int SEEK = 4;
    public static final int VOLUME = 5;
    public static final int REPEAT = 6;
    public static final int SHUFFLE = 7;
    public static final int PLAY_INDEX = 8;
    public static final int REMOVE = 9;
    public static final int MOVE_UP = 10;
    public static final int MOVE_DOWN = 11;
    public static final int CLEAR = 12;

    /** The screen can be used from a few blocks away, like a chest. */
    static final double MAX_DISTANCE_SQR = 64.0D;

    private final BlockPos pos;
    private final int action;
    private final long value;

    public MixerActionC2SPacket(BlockPos pos, int action, long value) {
        this.pos = pos;
        this.action = action;
        this.value = value;
    }

    public static void encode(MixerActionC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeVarInt(packet.action);
        buf.writeVarLong(packet.value);
    }

    public static MixerActionC2SPacket decode(FriendlyByteBuf buf) {
        return new MixerActionC2SPacket(buf.readBlockPos(), buf.readVarInt(), buf.readVarLong());
    }

    public static void handle(MixerActionC2SPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DjMixerBlockEntity mixer = mixerFor(context.getSender(), packet.pos);
            if (mixer == null) {
                return;
            }
            int index = (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, packet.value));
            switch (packet.action) {
                case PLAY_PAUSE -> mixer.playPause();
                case STOP -> mixer.stop();
                case NEXT -> mixer.next();
                case PREVIOUS -> mixer.previous();
                case SEEK -> mixer.seek(packet.value);
                case VOLUME -> mixer.setVolume(index);
                case REPEAT -> mixer.cycleRepeat();
                case SHUFFLE -> mixer.toggleShuffle();
                case PLAY_INDEX -> mixer.playIndex(index);
                case REMOVE -> mixer.remove(index);
                case MOVE_UP -> mixer.move(index, -1);
                case MOVE_DOWN -> mixer.move(index, 1);
                case CLEAR -> mixer.clear();
                default -> { }
            }
        });
        context.setPacketHandled(true);
    }

    static DjMixerBlockEntity mixerFor(ServerPlayer player, BlockPos pos) {
        if (player == null || player.isSpectator() || player.distanceToSqr(pos.getCenter()) > MAX_DISTANCE_SQR
                || !player.level().isLoaded(pos) || !(player.level().getBlockEntity(pos) instanceof DjMixerBlockEntity mixer)) {
            return null;
        }
        return mixer;
    }
}
