package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Rolls the camera half a turn while the Evil Lever has the screen flipped. The mouse is left as it is. */
@Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ScreenFlipClient {
    private static boolean flipped;

    private ScreenFlipClient() {}

    public static void set(boolean value) {
        flipped = value;
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (flipped) {
            event.setRoll(event.getRoll() + 180.0F);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        flipped = false;
    }
}
