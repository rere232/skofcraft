package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.block.LaunchPadBlock;
import ch.rere232.skofcraft.blockentity.LaunchPadBlockEntity;
import ch.rere232.skofcraft.entity.IcbmWarhead;
import ch.rere232.skofcraft.entity.LaunchRocket;
import com.mojang.math.Axis;
import ch.rere232.skofcraft.entity.OrbitalRod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Renderers for the launch rocket (on the pad and in flight) and the falling tungsten rod. */
public final class SpaceRenderers {
    private SpaceRenderers() {}

    /** The Giga ICBM is the launcher rocket airframe, bigger and painted for war. */
    public static final float ICBM_SCALE = 1.6F;

    public static class RocketRenderer extends EntityRenderer<LaunchRocket> {
        private final ModelPart model;
        private final ResourceLocation texture;
        private final float scale;

        public RocketRenderer(EntityRendererProvider.Context context) {
            this(context, SpaceModels.ROCKET_TEXTURE, 1.0F);
        }

        private RocketRenderer(EntityRendererProvider.Context context, ResourceLocation texture, float scale) {
            super(context);
            this.model = context.bakeLayer(SpaceModels.ROCKET);
            this.texture = texture;
            this.scale = scale;
            this.shadowRadius = 0.6F * scale;
        }

        public static RocketRenderer icbm(EntityRendererProvider.Context context) {
            return new RocketRenderer(context, SpaceModels.ICBM_TEXTURE, ICBM_SCALE);
        }

        @Override
        public void render(LaunchRocket rocket, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
            // Lit by its own engine once it is burning
            int packed = rocket.timer() >= rocket.countdown() ? LightTexture.FULL_BRIGHT : light;
            poseStack.pushPose();
            poseStack.scale(scale, scale, scale);
            model.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), packed, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            super.render(rocket, yaw, partialTick, poseStack, buffers, light);
        }

        @Override
        public ResourceLocation getTextureLocation(LaunchRocket rocket) {
            return texture;
        }
    }

    /** The ICBM falling nose first: its tip is at the entity position. */
    public static class WarheadRenderer extends EntityRenderer<IcbmWarhead> {
        private final ModelPart model;

        public WarheadRenderer(EntityRendererProvider.Context context) {
            super(context);
            this.model = context.bakeLayer(SpaceModels.ROCKET);
        }

        @Override
        public void render(IcbmWarhead warhead, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
            poseStack.pushPose();
            poseStack.translate(0.0D, SpaceModels.ROCKET_HEIGHT * ICBM_SCALE, 0.0D);
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            poseStack.scale(ICBM_SCALE, ICBM_SCALE, ICBM_SCALE);
            model.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(SpaceModels.ICBM_TEXTURE)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            super.render(warhead, yaw, partialTick, poseStack, buffers, light);
        }

        @Override
        public ResourceLocation getTextureLocation(IcbmWarhead warhead) {
            return SpaceModels.ICBM_TEXTURE;
        }
    }

    public static class RodRenderer extends EntityRenderer<OrbitalRod> {
        private final ModelPart model;

        public RodRenderer(EntityRendererProvider.Context context) {
            super(context);
            this.model = context.bakeLayer(SpaceModels.ROD);
        }

        @Override
        public void render(OrbitalRod rod, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
            // White-hot from re-entry
            model.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(SpaceModels.ROD_TEXTURE)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            super.render(rod, yaw, partialTick, poseStack, buffers, light);
        }

        @Override
        public ResourceLocation getTextureLocation(OrbitalRod rod) {
            return SpaceModels.ROD_TEXTURE;
        }
    }

    /** Shows the rocket standing on the pad as soon as a satellite is loaded. */
    public static class PadRenderer implements BlockEntityRenderer<LaunchPadBlockEntity> {
        private final ModelPart model;

        public PadRenderer(BlockEntityRendererProvider.Context context) {
            this.model = context.bakeLayer(SpaceModels.ROCKET);
        }

        @Override
        public void render(LaunchPadBlockEntity pad, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
            if (!pad.hasSatellite()) {
                return;
            }
            poseStack.pushPose();
            poseStack.translate(0.5D, LaunchPadBlock.TOP, 0.5D);
            boolean icbm = pad.hasIcbm();
            if (icbm) {
                poseStack.scale(ICBM_SCALE, ICBM_SCALE, ICBM_SCALE);
            }
            model.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(icbm ? SpaceModels.ICBM_TEXTURE : SpaceModels.ROCKET_TEXTURE)), light, overlay);
            poseStack.popPose();
        }

        @Override
        public boolean shouldRenderOffScreen(LaunchPadBlockEntity pad) {
            return true;
        }

        @Override
        public int getViewDistance() {
            return 256;
        }
    }
}
