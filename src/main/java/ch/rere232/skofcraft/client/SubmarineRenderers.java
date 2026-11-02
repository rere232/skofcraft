package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.NuclearSubmarine;
import ch.rere232.skofcraft.entity.Torpedo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Draws the submarine and its torpedo. */
public final class SubmarineRenderers {
    private SubmarineRenderers() {}

    public static class BoatRenderer extends EntityRenderer<NuclearSubmarine> {
        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "textures/entity/nuclear_submarine.png");
        private static final float MODEL_SCALE = 2.0F;
        private static final float BOB_HEIGHT = 0.05F;
        private static final float BOB_SPEED = 0.06F;

        private final SubmarineModels.Boat model;

        public BoatRenderer(EntityRendererProvider.Context context) {
            super(context);
            this.model = new SubmarineModels.Boat(context.bakeLayer(SubmarineModels.Boat.LAYER));
            this.shadowRadius = 0.0F;
        }

        @Override
        public void render(NuclearSubmarine boat, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
            poseStack.pushPose();
            if (boat.depth() < 0.5D) {
                // Riding the swell, on the surface
                poseStack.translate(0.0F, Mth.sin((boat.tickCount + partialTick) * BOB_SPEED) * BOB_HEIGHT, 0.0F);
            }
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
            poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.translate(0.0F, -1.501F, 0.0F);
            model.setupAnim(boat, 0.0F, 0.0F, boat.tickCount + partialTick, 0.0F, 0.0F);
            model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutout(TEXTURE)), light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
            super.render(boat, yaw, partialTick, poseStack, buffer, light);
        }

        @Override
        public ResourceLocation getTextureLocation(NuclearSubmarine boat) {
            return TEXTURE;
        }
    }

    public static class FishRenderer extends EntityRenderer<Torpedo> {
        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "textures/entity/torpedo.png");

        private final SubmarineModels.Fish model;

        public FishRenderer(EntityRendererProvider.Context context) {
            super(context);
            this.model = new SubmarineModels.Fish(context.bakeLayer(SubmarineModels.Fish.LAYER));
        }

        @Override
        public void render(Torpedo torpedo, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick, torpedo.yRotO, torpedo.getYRot())));
            poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, torpedo.xRotO, torpedo.getXRot())));
            model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutout(TEXTURE)), light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
            super.render(torpedo, yaw, partialTick, poseStack, buffer, light);
        }

        @Override
        public ResourceLocation getTextureLocation(Torpedo torpedo) {
            return TEXTURE;
        }
    }
}
