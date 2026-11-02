package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.Postbus;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class PostbusRenderer extends EntityRenderer<Postbus> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "textures/entity/postbus.png");
    private static final ResourceLocation INTERIOR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "textures/entity/postbus_interior.png");
    private static final ResourceLocation DASH_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "textures/entity/postbus_dash.png");
    private static final float MODEL_SCALE = 2.0F;
    /** Model units to the pose's units, which is what the model parts do to their own vertices. */
    private static final float UNIT = 1.0F / 16.0F;
    /** Makes one line of the font one model unit tall. */
    private static final float TEXT_SCALE = UNIT / 8.0F;
    /** The ceiling lights: the cabin never gets darker than this at night. */
    private static final int CABIN_LIGHT = 11;
    private static final int BOARD_MESSAGES = 4;
    private static final int TICKS_PER_MESSAGE = 80;
    private static final int KMH_AT_ONE_BLOCK_PER_TICK = 72;
    private static final float LOW_FUEL = 0.1F;
    private static final int AMBER = 0xFFFFA91E;
    private static final int RED = 0xFFE03030;

    private final PostbusModel model;
    private final PostbusInteriorModel interior;

    public PostbusRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new PostbusModel(context.bakeLayer(PostbusModel.LAYER));
        this.interior = new PostbusInteriorModel(context.bakeLayer(PostbusInteriorModel.LAYER));
        this.shadowRadius = 2.0F;
    }

    @Override
    public void render(Postbus bus, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        model.setupAnim(bus, 0.0F, 0.0F, bus.tickCount + partialTick, 0.0F, 0.0F);
        // Culled on purpose: from inside the bus the body's walls vanish and only the cabin's panels are left
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(TEXTURE));
        model.renderToBuffer(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        // The body is opaque, so nobody outside could see the cabin anyway
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.getVehicle() == bus) {
            renderCabin(bus, partialTick, poseStack, buffer, light);
        }
        poseStack.popPose();
        super.render(bus, yaw, partialTick, poseStack, buffer, light);
    }

    private void renderCabin(Postbus bus, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
        int cabinLight = LightTexture.pack(Math.max(LightTexture.block(light), CABIN_LIGHT), LightTexture.sky(light));
        interior.setupAnim(bus, 0.0F, 0.0F, bus.tickCount + partialTick, 0.0F, 0.0F);
        interior.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutout(INTERIOR_TEXTURE)), cabinLight,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        drawCluster(poseStack, buffer.getBuffer(RenderType.entityCutout(DASH_TEXTURE)));

        Font font = Minecraft.getInstance().font;
        float centre = (PostbusInteriorModel.CLUSTER_LEFT + PostbusInteriorModel.CLUSTER_RIGHT) / 2.0F;
        float clusterFace = PostbusInteriorModel.CLUSTER_FACE + 0.05F;
        int kmh = Math.round(bus.shownSpeed * KMH_AT_ONE_BLOCK_PER_TICK);
        drawText(poseStack, buffer, font, Component.literal(Integer.toString(kmh)), centre, 9.0F, clusterFace, 0.7F, 1.6F, AMBER);
        if (bus.fuelFraction() <= LOW_FUEL) {
            drawText(poseStack, buffer, font, Component.translatable("hud.skofcraft.apache.fuel"), centre, 9.8F, clusterFace, 0.5F, 1.6F, RED);
        }
        drawText(poseStack, buffer, font, boardMessage(bus), 0.0F, PostbusInteriorModel.BOARD_TOP + 0.5F,
                PostbusInteriorModel.BOARD_FACE + 0.02F, 1.0F, PostbusInteriorModel.BOARD_WIDTH - 1.0F, AMBER);
    }

    /** The clock comes up first, every time: this is the part of a Swiss bus that matters. */
    private static Component boardMessage(Postbus bus) {
        int message = (bus.tickCount / TICKS_PER_MESSAGE) % BOARD_MESSAGES;
        if (message == 0) {
            long time = bus.level().getDayTime() % 24000L;
            String clock = String.format("%02d:%02d", (time / 1000L + 6L) % 24L, time % 1000L * 60L / 1000L);
            return Component.translatable("screen.skofcraft.postbus.on_time", clock);
        }
        return Component.translatable("screen.skofcraft.postbus." + message);
    }

    /** Gauges at four times the density of the rest of the cabin, which a model part's texture can't do. */
    private static void drawCluster(PoseStack poseStack, VertexConsumer consumer) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        float left = PostbusInteriorModel.CLUSTER_LEFT * UNIT;
        float right = PostbusInteriorModel.CLUSTER_RIGHT * UNIT;
        float top = PostbusInteriorModel.CLUSTER_TOP * UNIT;
        float bottom = PostbusInteriorModel.CLUSTER_BOTTOM * UNIT;
        float z = (PostbusInteriorModel.CLUSTER_FACE + 0.03F) * UNIT;
        vertex(consumer, pose, normal, right, top, z, 1.0F, 0.0F);
        vertex(consumer, pose, normal, left, top, z, 0.0F, 0.0F);
        vertex(consumer, pose, normal, left, bottom, z, 0.0F, 1.0F);
        vertex(consumer, pose, normal, right, bottom, z, 1.0F, 1.0F);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, float x, float y, float z, float u, float v) {
        consumer.vertex(pose, x, y, z).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
    }

    /** Text on a surface facing the back of the bus, centred on x, shrunk if it wouldn't fit in maxWidth model units. */
    private static void drawText(PoseStack poseStack, MultiBufferSource buffer, Font font, Component text, float x, float y, float z,
                                 float size, float maxWidth, int color) {
        int width = font.width(text);
        float scale = Math.min(size, maxWidth / (width * TEXT_SCALE / UNIT)) * TEXT_SCALE;
        poseStack.pushPose();
        poseStack.translate(x * UNIT, y * UNIT, z * UNIT);
        // +X is to the left as you look forward, so the text has to run the other way
        poseStack.scale(-scale, scale, scale);
        font.drawInBatch(text, -width / 2.0F, 0.0F, color, false, poseStack.last().pose(), buffer,
                Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(Postbus bus) {
        return TEXTURE;
    }
}
