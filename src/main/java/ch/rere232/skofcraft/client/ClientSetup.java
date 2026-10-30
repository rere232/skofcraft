package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.blockentity.SkofcraftBlockEntities;
import ch.rere232.skofcraft.menu.FEDryerMenu;
import ch.rere232.skofcraft.menu.FEGrinderMenu;
import ch.rere232.skofcraft.menu.FEMixerMenu;
import ch.rere232.skofcraft.menu.FEPressMenu;
import ch.rere232.skofcraft.menu.FEPackagerMenu;
import ch.rere232.skofcraft.menu.SkofcraftMenus;
import ch.rere232.skofcraft.registry.SkofcraftEntities;
import ch.rere232.skofcraft.screen.FEDryerScreen;
import ch.rere232.skofcraft.screen.FEGrinderScreen;
import ch.rere232.skofcraft.screen.FEMixerScreen;
import ch.rere232.skofcraft.screen.FEPressScreen;
import ch.rere232.skofcraft.screen.FEPackagerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;

@Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void setupClient(FMLClientSetupEvent event) {
        MenuScreens.register(SkofcraftMenus.FE_DRYER.get(), FEDryerScreen::new);
        MenuScreens.register(SkofcraftMenus.FE_GRINDER.get(), FEGrinderScreen::new);
        MenuScreens.register(SkofcraftMenus.FE_MIXER.get(), FEMixerScreen::new);
        MenuScreens.register(SkofcraftMenus.FE_PRESS.get(), FEPressScreen::new);
        MenuScreens.register(SkofcraftMenus.FE_PACKAGER.get(), FEPackagerScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SkofcraftEntities.SPIT.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(SkofcraftEntities.DELIVERY_PLANE.get(), DeliveryPlaneRenderer::new);
        event.registerEntityRenderer(SkofcraftEntities.DELIVERY_CRATE.get(), DeliveryCrateRenderer::new);
        event.registerEntityRenderer(SkofcraftEntities.APACHE.get(), ApacheRenderer::new);
        event.registerEntityRenderer(SkofcraftEntities.F14.get(), F14Renderer::new);
        event.registerEntityRenderer(SkofcraftEntities.APACHE_BULLET.get(), ctx -> new ThrownItemRenderer<>(ctx, 0.6F, true));
        event.registerEntityRenderer(SkofcraftEntities.APACHE_ROCKET.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.0F, true));
        event.registerBlockEntityRenderer(SkofcraftBlockEntities.CHICHA.get(), ChichaRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DeliveryPlaneModel.LAYER, DeliveryPlaneModel::createBodyLayer);
        event.registerLayerDefinition(DeliveryCrateModel.LAYER, DeliveryCrateModel::createBodyLayer);
        event.registerLayerDefinition(ApacheModel.LAYER, ApacheModel::createBodyLayer);
        event.registerLayerDefinition(F14Model.LAYER, F14Model::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("spit_overlay", SpitOverlay::render);
        event.registerAboveAll("apache_hud", ApacheHud::render);
        event.registerAboveAll("f14_hud", F14Hud::render);
    }
}
