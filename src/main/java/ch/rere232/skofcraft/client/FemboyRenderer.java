package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.Femboy;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** {@link FemboyModel} with the skin of its variant, drawn at a player's scale so it matches the hitbox. */
public class FemboyRenderer extends HumanoidMobRenderer<Femboy, FemboyModel> {
    private static final float PLAYER_SCALE = 0.9375F;
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[Femboy.VARIANTS];

    static {
        for (int i = 0; i < Femboy.VARIANTS; i++) {
            TEXTURES[i] = ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "textures/entity/femboy/femboy_" + i + ".png");
        }
    }

    public FemboyRenderer(EntityRendererProvider.Context context) {
        super(context, new FemboyModel(context.bakeLayer(FemboyModel.LAYER)), 0.5F);
    }

    @Override
    protected void scale(Femboy femboy, PoseStack poseStack, float partialTicks) {
        poseStack.scale(PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(Femboy femboy) {
        return TEXTURES[Math.floorMod(femboy.getVariant(), Femboy.VARIANTS)];
    }
}
