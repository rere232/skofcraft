package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.client.RadarClient;
import ch.rere232.skofcraft.radar.RadarContact;
import ch.rere232.skofcraft.radar.RadarMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** The result of a radar sweep, for the screen the player has open on that radar. */
public class RadarContactsS2CPacket {
    public final BlockPos pos;
    public final RadarMode mode;
    public final int range;
    public final boolean powered;
    public final int energy;
    public final int signal;
    public final List<RadarContact> contacts;

    public RadarContactsS2CPacket(BlockPos pos, RadarMode mode, int range, boolean powered, int energy, int signal, List<RadarContact> contacts) {
        this.pos = pos;
        this.mode = mode;
        this.range = range;
        this.powered = powered;
        this.energy = energy;
        this.signal = signal;
        this.contacts = contacts;
    }

    public static void encode(RadarContactsS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeByte(packet.mode.ordinal());
        buf.writeVarInt(packet.range);
        buf.writeBoolean(packet.powered);
        buf.writeVarInt(packet.energy);
        buf.writeByte(packet.signal);
        buf.writeVarInt(packet.contacts.size());
        packet.contacts.forEach(contact -> contact.write(buf));
    }

    public static RadarContactsS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        RadarMode mode = RadarMode.byId(buf.readByte());
        int range = buf.readVarInt();
        boolean powered = buf.readBoolean();
        int energy = buf.readVarInt();
        int signal = buf.readByte();
        int count = Math.min(buf.readVarInt(), 256);
        List<RadarContact> contacts = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            contacts.add(RadarContact.read(buf));
        }
        return new RadarContactsS2CPacket(pos, mode, range, powered, energy, signal, contacts);
    }

    public static void handle(RadarContactsS2CPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RadarClient.receive(packet)));
        context.setPacketHandled(true);
    }
}
