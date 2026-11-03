package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** A person-shaped mob in Pastel Paradise, at any size. */
public class PastelMobRenderer<T extends Mob> extends HumanoidMobRenderer<T, PlayerModel<T>> {
    private final ResourceLocation texture;
    private final float scale;

    public PastelMobRenderer(EntityRendererProvider.Context context, String skin, float scale) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F * scale);
        this.texture = ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "textures/entity/" + skin + ".png");
        this.scale = scale;
    }

    @Override
    protected void scale(T mob, PoseStack poseStack, float partialTick) {
        poseStack.scale(scale, scale, scale);
    }

    @Override
    public ResourceLocation getTextureLocation(T mob) {
        return texture;
    }
}
