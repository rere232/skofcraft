package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.Postbus;
import ch.rere232.skofcraft.keybind.SkofcraftKeybinds;
import ch.rere232.skofcraft.network.PostbusHornC2SPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The driver's side of the postbus: the horn key, and a clear view of the road and the dashboard. */
@Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PostbusClient {
    private PostbusClient() {}

    private static boolean isDriving() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getVehicle() instanceof Postbus bus && bus.getControllingPassenger() == mc.player;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || SkofcraftKeybinds.POSTBUS_HORN == null) {
            return;
        }
        while (SkofcraftKeybinds.POSTBUS_HORN.consumeClick()) {
            if (isDriving()) {
                SkofcraftNetwork.CHANNEL.sendToServer(new PostbusHornC2SPacket());
            }
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (isDriving()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type() && isDriving()) {
            event.setCanceled(true);
        }
    }
}
