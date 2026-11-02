package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.screen.HrTrainingScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Keeps the training screen open: whatever else opens over it, it comes straight back, on the same page. */
@Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class HrClient {
    private static HrTrainingScreen screen;

    private HrClient() {}

    public static void set(boolean active) {
        Minecraft mc = Minecraft.getInstance();
        if (active) {
            screen = new HrTrainingScreen();
            mc.setScreen(screen);
        } else {
            if (mc.screen == screen) {
                mc.setScreen(null);
            }
            screen = null;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase == TickEvent.Phase.END && screen != null && mc.screen != screen
                && mc.player != null && !mc.player.isDeadOrDying()) {
            mc.setScreen(screen);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        screen = null;
    }
}
