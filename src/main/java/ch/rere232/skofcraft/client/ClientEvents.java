package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.keybind.SkofcraftKeybinds;
import ch.rere232.skofcraft.screen.GumScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.Minecraft;

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
    }
}
