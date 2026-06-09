package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.SkofcraftMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class SkofcraftNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.registerMessage(packetId++, GumSyncS2CPacket.class, GumSyncS2CPacket::encode, GumSyncS2CPacket::decode, GumSyncS2CPacket::handle);
    }
}
