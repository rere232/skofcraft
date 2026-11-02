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
import ch.rere232.skofcraft.registry.SkofcraftItems;
import ch.rere232.skofcraft.item.MaskItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import ch.rere232.skofcraft.screen.AircraftWorkbenchScreen;
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
        MenuScreens.register(SkofcraftMenus.AIRCRAFT_WORKBENCH.get(), AircraftWorkbenchScreen::new);
        CuriosRendererRegistry.register(SkofcraftItems.CHOKER.get(), () -> new ClothingRenderer(ClothingRenderer.Piece.CHOKER));
        CuriosRendererRegistry.register(SkofcraftItems.SKIRT.get(), () -> new ClothingRenderer(ClothingRenderer.Piece.SKIRT));
        CuriosRendererRegistry.register(SkofcraftItems.THIGH_HIGHS.get(), () -> new ClothingRenderer(ClothingRenderer.Piece.THIGH_HIGHS));
        CuriosRendererRegistry.register(SkofcraftItems.CROP_TOP.get(), () -> new ClothingRenderer(ClothingRenderer.Piece.CROP_TOP));
        CuriosRendererRegistry.register(SkofcraftItems.HAND_WARMERS.get(), () -> new ClothingRenderer(ClothingRenderer.Piece.HAND_WARMERS));
        CuriosRendererRegistry.register(SkofcraftItems.FACE_MASK.get(), () -> new ClothingRenderer(ClothingRenderer.Piece.MASK));
        CuriosRendererRegistry.register(SkofcraftItems.CAT_EARS.get(), () -> new ClothingRenderer(ClothingRenderer.Piece.CAT_EARS));
        CuriosRendererRegistry.register(SkofcraftItems.CAT_TAIL.get(), () -> new ClothingRenderer(ClothingRenderer.Piece.CAT_TAIL));
        CuriosRendererRegistry.register(SkofcraftItems.TACTICAL_GLOVES.get(), () -> new ClothingRenderer(ClothingRenderer.Piece.TACTICAL_GLOVES));
        // The face mask icon follows its style: 0, 1/3, 2/3, 1 (item properties are clamped to 0..1)
        event.enqueueWork(() -> ItemProperties.register(SkofcraftItems.FACE_MASK.get(),
                ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "style"),
                (stack, level, entity, seed) -> MaskItem.style(stack).ordinal() / (float) (MaskItem.Style.values().length - 1)));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SkofcraftEntities.SPIT.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(SkofcraftEntities.FEMBOY.get(), FemboyRenderer::new);
        event.registerEntityRenderer(SkofcraftEntities.ANIME_CREW.get(), AnimeCrewRenderer::new);
        event.registerEntityRenderer(SkofcraftEntities.HR_REPRESENTATIVE.get(), HrRepresentativeRenderer::new);
        event.registerEntityRenderer(SkofcraftEntities.DELIVERY_PLANE.get(), DeliveryPlaneRenderer::new);
        event.registerEntityRenderer(SkofcraftEntities.DELIVERY_CRATE.get(), DeliveryCrateRenderer::new);
        event.registerEntityRenderer(SkofcraftEntities.APACHE.get(), ApacheRenderer::new);
        event.registerEntityRenderer(SkofcraftEntities.F14.get(), F14Renderer::new);
        event.registerEntityRenderer(SkofcraftEntities.MAUS.get(), ctx -> new TankRenderer<>(ctx, new MausModel(ctx.bakeLayer(MausModel.LAYER)), "maus", 3.0F));
        event.registerEntityRenderer(SkofcraftEntities.LEOPARD_1.get(), ctx -> new TankRenderer<>(ctx, new Leopard1Model(ctx.bakeLayer(Leopard1Model.LAYER)), "leopard_1", 2.5F));
        event.registerEntityRenderer(SkofcraftEntities.TIGER_2.get(), ctx -> new TankRenderer<>(ctx, new Tiger2Model(ctx.bakeLayer(Tiger2Model.LAYER)), "tiger_2", 2.8F));
        event.registerEntityRenderer(SkofcraftEntities.TANK_SHELL.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.0F, true));
        event.registerEntityRenderer(SkofcraftEntities.POSTBUS.get(), PostbusRenderer::new);
        event.registerEntityRenderer(SkofcraftEntities.AIRCRAFT_CARRIER.get(), AircraftCarrierRenderer::new);
        event.registerEntityRenderer(SkofcraftEntities.APACHE_BULLET.get(), ctx -> new ThrownItemRenderer<>(ctx, 0.6F, true));
        event.registerEntityRenderer(SkofcraftEntities.APACHE_ROCKET.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.0F, true));
        event.registerBlockEntityRenderer(SkofcraftBlockEntities.CHICHA.get(), ChichaRenderer::new);
        event.registerBlockEntityRenderer(SkofcraftBlockEntities.BIG_RED_BUTTON.get(), BigRedButtonRenderer::new);
        event.registerBlockEntityRenderer(SkofcraftBlockEntities.DEFENSE_TURRET.get(), DefenseTurretRenderer::new);
        event.registerBlockEntityRenderer(SkofcraftBlockEntities.LAUNCH_PAD.get(), SpaceRenderers.PadRenderer::new);
        event.registerEntityRenderer(SkofcraftEntities.LAUNCH_ROCKET.get(), SpaceRenderers.RocketRenderer::new);
        event.registerEntityRenderer(SkofcraftEntities.ORBITAL_ROD.get(), SpaceRenderers.RodRenderer::new);
        event.registerEntityRenderer(SkofcraftEntities.GIGA_ICBM.get(), SpaceRenderers.RocketRenderer::icbm);
        event.registerEntityRenderer(SkofcraftEntities.ICBM_WARHEAD.get(), SpaceRenderers.WarheadRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DeliveryPlaneModel.LAYER, DeliveryPlaneModel::createBodyLayer);
        event.registerLayerDefinition(DeliveryCrateModel.LAYER, DeliveryCrateModel::createBodyLayer);
        event.registerLayerDefinition(ApacheModel.LAYER, ApacheModel::createBodyLayer);
        event.registerLayerDefinition(F14Model.LAYER, F14Model::createBodyLayer);
        event.registerLayerDefinition(MausModel.LAYER, MausModel::createBodyLayer);
        event.registerLayerDefinition(Leopard1Model.LAYER, Leopard1Model::createBodyLayer);
        event.registerLayerDefinition(Tiger2Model.LAYER, Tiger2Model::createBodyLayer);
        event.registerLayerDefinition(PostbusModel.LAYER, PostbusModel::createBodyLayer);
        event.registerLayerDefinition(PostbusInteriorModel.LAYER, PostbusInteriorModel::createBodyLayer);
        event.registerLayerDefinition(DefenseTurretModel.CIWS_LAYER, DefenseTurretModel::createCiwsLayer);
        event.registerLayerDefinition(DefenseTurretModel.SAM_LAYER, DefenseTurretModel::createSamLayer);
        event.registerLayerDefinition(SpaceModels.ROCKET, SpaceModels::createRocketLayer);
        event.registerLayerDefinition(SpaceModels.ROD, SpaceModels::createRodLayer);
        event.registerLayerDefinition(TacticalArmorModel.OUTER, TacticalArmorModel::createOuterLayer);
        event.registerLayerDefinition(TacticalArmorModel.INNER, TacticalArmorModel::createInnerLayer);
        for (ClothingRenderer.Piece piece : ClothingRenderer.Piece.values()) {
            event.registerLayerDefinition(piece.layer, piece::createLayer);
        }
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("spit_overlay", SpitOverlay::render);
        event.registerAboveAll("apache_hud", ApacheHud::render);
        event.registerAboveAll("f14_hud", F14Hud::render);
        event.registerAboveAll("maus_hud", TankHud::render);
        event.registerAboveAll("nuclear_flash", NuclearBlastClient::renderFlash);
    }
}
