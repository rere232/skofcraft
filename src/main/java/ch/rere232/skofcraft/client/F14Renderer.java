package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.F14Tomcat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class F14Renderer extends EntityRenderer<F14Tomcat> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "textures/entity/f14.png");
    private static final float MODEL_SCALE = 2.5F;
    private static final float PIVOT_HEIGHT = 1.4F;

    private final F14Model model;

    public F14Renderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new F14Model(context.bakeLayer(F14Model.LAYER));
        this.shadowRadius = 2.0F;
    }

    @Override
    public void render(F14Tomcat jet, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int light) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.getCameraType().isFirstPerson() && mc.getCameraEntity() != null
                && mc.getCameraEntity().getVehicle() == jet) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.translate(0.0F, PIVOT_HEIGHT, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, jet.xRotO, jet.getXRot())));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, jet.tiltRollO, jet.tiltRoll)));
        poseStack.translate(0.0F, -PIVOT_HEIGHT, 0.0F);
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        model.setupAnim(jet, 0.0F, 0.0F, jet.tickCount + partialTick, 0.0F, 0.0F);
        VertexConsumer consumer = buffer.getBuffer(model.renderType(TEXTURE));
        model.renderToBuffer(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
        super.render(jet, yaw, partialTick, poseStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(F14Tomcat jet) {
        return TEXTURE;
    }
}
