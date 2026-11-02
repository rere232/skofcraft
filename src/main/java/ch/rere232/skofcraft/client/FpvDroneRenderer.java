package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.FpvDrone;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class FpvDroneRenderer extends EntityRenderer<FpvDrone> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "textures/entity/fpv_drone.png");
    private static final float PIVOT_HEIGHT = 0.25F;

    private final FpvDroneModel model;

    public FpvDroneRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new FpvDroneModel(context.bakeLayer(FpvDroneModel.LAYER));
        this.shadowRadius = 0.3F;
    }

    @Override
    public void render(FpvDrone drone, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.translate(0.0F, PIVOT_HEIGHT, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, drone.tiltPitchO, drone.tiltPitch)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, drone.tiltRollO, drone.tiltRoll)));
        poseStack.translate(0.0F, -PIVOT_HEIGHT, 0.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        model.setProps(Mth.lerp(partialTick, drone.propAngleO, drone.propAngle));
        VertexConsumer consumer = buffer.getBuffer(model.renderType(TEXTURE));
        model.renderToBuffer(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
        super.render(drone, yaw, partialTick, poseStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(FpvDrone drone) {
        return TEXTURE;
    }
}
