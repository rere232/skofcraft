package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.menu.FEDryerMenu;
import ch.rere232.skofcraft.menu.SkofcraftMenus;
import ch.rere232.skofcraft.screen.FEDryerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.gui.screens.MenuScreens;

@Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void setupClient(FMLClientSetupEvent event) {
        MenuScreens.register(SkofcraftMenus.FE_DRYER.get(), FEDryerScreen::new);
    }
}
