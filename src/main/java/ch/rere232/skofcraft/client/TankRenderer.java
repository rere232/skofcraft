package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.Tank;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class TankRenderer<T extends Tank> extends EntityRenderer<T> {
    private static final float MODEL_SCALE = 2.0F;

    private final TankModel model;
    private final ResourceLocation texture;

    public TankRenderer(EntityRendererProvider.Context context, TankModel model, String textureName, float shadowRadius) {
        super(context);
        this.model = model;
        this.texture = ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "textures/entity/" + textureName + ".png");
        this.shadowRadius = shadowRadius;
    }

    @Override
    public void render(T tank, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
        // From the driver's hatch the turret would fill the screen, so in first person they get the sight instead
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson() && AircraftClient.piloted() == tank) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        model.setupAnim(tank, 0.0F, 0.0F, tank.tickCount + partialTick, 0.0F, 0.0F);
        model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutout(texture)), light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
        super.render(tank, yaw, partialTick, poseStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(T tank) {
        return texture;
    }
}
