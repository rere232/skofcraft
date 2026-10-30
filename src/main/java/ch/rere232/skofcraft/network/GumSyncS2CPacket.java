package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.client.GumClientSync;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GumSyncS2CPacket {
    private static final int SLOT_COUNT = 4;

    private final int[] ticks;
    private final String[] itemIds;
    private final String[] flavorIds;
    private final int[] strengths;
    private final int tolerance;
    private final int addiction;

    public GumSyncS2CPacket(int[] ticks, String[] itemIds, String[] flavorIds, int[] strengths, int tolerance, int addiction) {
        this.ticks = ticks;
        this.itemIds = itemIds;
        this.flavorIds = flavorIds;
        this.strengths = strengths;
        this.tolerance = tolerance;
        this.addiction = addiction;
    }

    public static void encode(GumSyncS2CPacket packet, FriendlyByteBuf buf) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            buf.writeVarInt(packet.ticks[i]);
            buf.writeUtf(packet.itemIds[i] == null ? "" : packet.itemIds[i]);
            buf.writeUtf(packet.flavorIds[i] == null ? "" : packet.flavorIds[i]);
            buf.writeVarInt(packet.strengths[i]);
        }
        buf.writeVarInt(packet.tolerance);
        buf.writeVarInt(packet.addiction);
    }

    public static GumSyncS2CPacket decode(FriendlyByteBuf buf) {
        int[] ticks = new int[SLOT_COUNT];
        String[] itemIds = new String[SLOT_COUNT];
        String[] flavorIds = new String[SLOT_COUNT];
        int[] strengths = new int[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) {
            ticks[i] = buf.readVarInt();
            itemIds[i] = buf.readUtf();
            flavorIds[i] = buf.readUtf();
            strengths[i] = buf.readVarInt();
        }
        int tolerance = buf.readVarInt();
        int addiction = buf.readVarInt();
        return new GumSyncS2CPacket(ticks, itemIds, flavorIds, strengths, tolerance, addiction);
    }

    public static void handle(GumSyncS2CPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> GumClientSync.handle(packet.ticks, packet.itemIds, packet.flavorIds, packet.strengths, packet.tolerance, packet.addiction)));
        context.setPacketHandled(true);
    }
}
