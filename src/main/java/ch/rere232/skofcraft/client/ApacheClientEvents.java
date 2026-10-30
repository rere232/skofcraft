package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.PilotedAircraft;
import ch.rere232.skofcraft.network.ApacheFireC2SPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client side of piloting: fire keys, a hidden pilot and hand, and the cockpit replacing the crosshair. */
@Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ApacheClientEvents {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        PilotedAircraft aircraft = AircraftClient.piloted();
        if (event.phase != TickEvent.Phase.END || aircraft == null || mc.screen != null
                || !mc.mouseHandler.isMouseGrabbed() || mc.level == null) {
            return;
        }
        if (mc.options.keyAttack.isDown()) {
            SkofcraftNetwork.CHANNEL.sendToServer(new ApacheFireC2SPacket(PilotedAircraft.WEAPON_GUN));
        }
        if (mc.options.keyUse.isDown()) {
            SkofcraftNetwork.CHANNEL.sendToServer(new ApacheFireC2SPacket(PilotedAircraft.WEAPON_MISSILE));
            long now = mc.level.getGameTime();
            if (now - AircraftClient.lastSecondaryTick >= aircraft.secondaryCooldownTicks()) {
                AircraftClient.lastSecondaryTick = now;
            }
        }
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if ((event.isAttack() || event.isUseItem()) && AircraftClient.piloted() != null) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    /** Riders are invisible in the helicopter, seen from any camera. */
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (event.getEntity().getVehicle() instanceof PilotedAircraft) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (AircraftClient.piloted() != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type() && AircraftClient.piloted() != null) {
            event.setCanceled(true);
        }
    }
}
