package ch.rere232.skofcraft.keybind;

import ch.rere232.skofcraft.SkofcraftMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.KeyMapping;

@Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SkofcraftKeybinds {
    public static KeyMapping OPEN_GUMS_SCREEN;

    @SubscribeEvent
    public static void registerKeybinds(RegisterKeyMappingsEvent event) {
        OPEN_GUMS_SCREEN = new KeyMapping("key.skofcraft.open_gums", com.mojang.blaze3d.platform.InputConstants.KEY_G, "key.categories.gameplay");
        event.register(OPEN_GUMS_SCREEN);
    }
}
