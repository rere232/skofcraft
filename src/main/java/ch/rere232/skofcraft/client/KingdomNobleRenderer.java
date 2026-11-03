package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.KingdomNoble;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** The nobles and the regent share a body and differ in clothes. */
public class KingdomNobleRenderer<T extends KingdomNoble> extends HumanoidMobRenderer<T, PlayerModel<T>> {
    private final ResourceLocation texture;

    public KingdomNobleRenderer(EntityRendererProvider.Context context, String skin) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        texture = ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "textures/entity/" + skin + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(T noble) {
        return texture;
    }
}
