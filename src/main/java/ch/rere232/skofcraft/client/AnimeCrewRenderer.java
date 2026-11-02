package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.AnimeCrew;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Slim player model with one of the four crew skins. */
public class AnimeCrewRenderer extends HumanoidMobRenderer<AnimeCrew, PlayerModel<AnimeCrew>> {
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[AnimeCrew.VARIANTS];

    static {
        for (int i = 0; i < AnimeCrew.VARIANTS; i++) {
            TEXTURES[i] = ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "textures/entity/anime_crew/anime_crew_" + i + ".png");
        }
    }

    public AnimeCrewRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(AnimeCrew crew) {
        return TEXTURES[Math.floorMod(crew.getVariant(), AnimeCrew.VARIANTS)];
    }
}
