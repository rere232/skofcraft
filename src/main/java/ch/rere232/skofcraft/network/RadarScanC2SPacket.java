package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.blockentity.RadarBlockEntity;
import ch.rere232.skofcraft.radar.RadarCategory;
import ch.rere232.skofcraft.radar.RadarContact;
import ch.rere232.skofcraft.radar.RadarMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Supplier;

/** Sent by an open radar screen a few times a second; the server answers with a fresh sweep. A mode of -1 keeps the filter. */
public class RadarScanC2SPacket {
    private static final double MAX_DISTANCE_SQR = 64.0D;

    private final BlockPos pos;
    private final int mode;

    public RadarScanC2SPacket(BlockPos pos, int mode) {
        this.pos = pos;
        this.mode = mode;
    }

    public static void encode(RadarScanC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeVarInt(packet.mode);
    }

    public static RadarScanC2SPacket decode(FriendlyByteBuf buf) {
        return new RadarScanC2SPacket(buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(RadarScanC2SPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.distanceToSqr(packet.pos.getCenter()) > MAX_DISTANCE_SQR
                    || !player.level().isLoaded(packet.pos)
                    || !(player.level().getBlockEntity(packet.pos) instanceof RadarBlockEntity radar)) {
                return;
            }
            if (packet.mode >= 0) {
                radar.setMode(RadarMode.byId(packet.mode));
            }
            List<RadarContact> contacts = radar.scan(player);
            if (contacts.stream().anyMatch(contact -> contact.category() == RadarCategory.AIRCRAFT)) {
                SkofcraftAdvancements.award(player, SkofcraftAdvancements.RADAR_CONTACT);
            }
            SkofcraftNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RadarContactsS2CPacket(
                    packet.pos, radar.getMode(), radar.range(), radar.isPowered(), radar.getEnergyStored(), radar.getSignal(), contacts));
        });
        context.setPacketHandled(true);
    }
}
