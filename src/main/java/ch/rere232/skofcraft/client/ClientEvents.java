package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.NuclearSubmarine;
import ch.rere232.skofcraft.keybind.SkofcraftKeybinds;
import ch.rere232.skofcraft.network.ApacheFireC2SPacket;
import ch.rere232.skofcraft.network.CuddleC2SPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import ch.rere232.skofcraft.network.SpitC2SPacket;
import ch.rere232.skofcraft.screen.GumScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

@Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (SkofcraftKeybinds.OPEN_GUMS_SCREEN.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.setScreen(new GumScreen(mc.player));
            }
        }
        while (SkofcraftKeybinds.SPIT.consumeClick()) {
            SkofcraftNetwork.CHANNEL.sendToServer(new SpitC2SPacket());
        }
        while (SkofcraftKeybinds.SUBMARINE_LAUNCH.consumeClick()) {
            if (AircraftClient.piloted() instanceof NuclearSubmarine) {
                SkofcraftNetwork.CHANNEL.sendToServer(new ApacheFireC2SPacket(NuclearSubmarine.WEAPON_ICBM));
            }
        }
        while (SkofcraftKeybinds.CUDDLE.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                SkofcraftNetwork.CHANNEL.sendToServer(new CuddleC2SPacket(mc.crosshairPickEntity instanceof Player target ? target.getId() : -1));
            }
        }
    }
}
