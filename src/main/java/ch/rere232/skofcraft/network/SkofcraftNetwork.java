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
        CHANNEL.registerMessage(packetId++, GumRemoveC2SPacket.class, GumRemoveC2SPacket::encode, GumRemoveC2SPacket::decode, GumRemoveC2SPacket::handle);
        CHANNEL.registerMessage(packetId++, SpitC2SPacket.class, SpitC2SPacket::encode, SpitC2SPacket::decode, SpitC2SPacket::handle);
        CHANNEL.registerMessage(packetId++, AmazooonOrderC2SPacket.class, AmazooonOrderC2SPacket::encode, AmazooonOrderC2SPacket::decode, AmazooonOrderC2SPacket::handle);
        CHANNEL.registerMessage(packetId++, CasinoSpinC2SPacket.class, CasinoSpinC2SPacket::encode, CasinoSpinC2SPacket::decode, CasinoSpinC2SPacket::handle);
        CHANNEL.registerMessage(packetId++, CasinoResultS2CPacket.class, CasinoResultS2CPacket::encode, CasinoResultS2CPacket::decode, CasinoResultS2CPacket::handle);
        CHANNEL.registerMessage(packetId++, RouletteSpinC2SPacket.class, RouletteSpinC2SPacket::encode, RouletteSpinC2SPacket::decode, RouletteSpinC2SPacket::handle);
        CHANNEL.registerMessage(packetId++, RouletteResultS2CPacket.class, RouletteResultS2CPacket::encode, RouletteResultS2CPacket::decode, RouletteResultS2CPacket::handle);
        CHANNEL.registerMessage(packetId++, BlackjackActionC2SPacket.class, BlackjackActionC2SPacket::encode, BlackjackActionC2SPacket::decode, BlackjackActionC2SPacket::handle);
        CHANNEL.registerMessage(packetId++, BlackjackStateS2CPacket.class, BlackjackStateS2CPacket::encode, BlackjackStateS2CPacket::decode, BlackjackStateS2CPacket::handle);
        CHANNEL.registerMessage(packetId++, PhoneUseC2SPacket.class, PhoneUseC2SPacket::encode, PhoneUseC2SPacket::decode, PhoneUseC2SPacket::handle);
        CHANNEL.registerMessage(packetId++, PhoneAddictionS2CPacket.class, PhoneAddictionS2CPacket::encode, PhoneAddictionS2CPacket::decode, PhoneAddictionS2CPacket::handle);
        CHANNEL.registerMessage(packetId++, ApacheFireC2SPacket.class, ApacheFireC2SPacket::encode, ApacheFireC2SPacket::decode, ApacheFireC2SPacket::handle);
        CHANNEL.registerMessage(packetId++, JetCrashC2SPacket.class, JetCrashC2SPacket::encode, JetCrashC2SPacket::decode, JetCrashC2SPacket::handle);
        CHANNEL.registerMessage(packetId++, SkofeeBrewC2SPacket.class, SkofeeBrewC2SPacket::encode, SkofeeBrewC2SPacket::decode, SkofeeBrewC2SPacket::handle);
        CHANNEL.registerMessage(packetId++, FlightDeckC2SPacket.class, FlightDeckC2SPacket::encode, FlightDeckC2SPacket::decode, FlightDeckC2SPacket::handle);
        CHANNEL.registerMessage(packetId++, RadarScanC2SPacket.class, RadarScanC2SPacket::encode, RadarScanC2SPacket::decode, RadarScanC2SPacket::handle);
        CHANNEL.registerMessage(packetId++, RadarContactsS2CPacket.class, RadarContactsS2CPacket::encode, RadarContactsS2CPacket::decode, RadarContactsS2CPacket::handle);
        CHANNEL.registerMessage(packetId++, MushroomCloudS2CPacket.class, MushroomCloudS2CPacket::encode, MushroomCloudS2CPacket::decode, MushroomCloudS2CPacket::handle);
        CHANNEL.registerMessage(packetId++, SatelliteLinkC2SPacket.class, SatelliteLinkC2SPacket::encode, SatelliteLinkC2SPacket::decode, SatelliteLinkC2SPacket::handle);
        CHANNEL.registerMessage(packetId++, SatelliteLinkS2CPacket.class, SatelliteLinkS2CPacket::encode, SatelliteLinkS2CPacket::decode, SatelliteLinkS2CPacket::handle);
    }
}
