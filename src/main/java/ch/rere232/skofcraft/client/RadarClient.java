package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.network.RadarContactsS2CPacket;
import ch.rere232.skofcraft.screen.RadarScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class RadarClient {
    private RadarClient() {}

    public static void open(BlockPos pos) {
        Minecraft.getInstance().setScreen(new RadarScreen(pos));
    }

    public static void receive(RadarContactsS2CPacket packet) {
        if (Minecraft.getInstance().screen instanceof RadarScreen screen && screen.pos().equals(packet.pos)) {
            screen.update(packet);
        }
    }
}
