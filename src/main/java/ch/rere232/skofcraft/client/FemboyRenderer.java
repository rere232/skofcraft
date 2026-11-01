package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.Femboy;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Slim player model with one of the femboy skins. */
public class FemboyRenderer extends HumanoidMobRenderer<Femboy, PlayerModel<Femboy>> {
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[Femboy.VARIANTS];

    static {
        for (int i = 0; i < Femboy.VARIANTS; i++) {
            TEXTURES[i] = ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "textures/entity/femboy/femboy_" + i + ".png");
        }
    }

    public FemboyRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(Femboy femboy) {
        return TEXTURES[Math.floorMod(femboy.getVariant(), Femboy.VARIANTS)];
    }
}
