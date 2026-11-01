package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.blockentity.BigRedButtonBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** The giant warning floating above the button, always facing the viewer and blinking. */
public class BigRedButtonRenderer implements BlockEntityRenderer<BigRedButtonBlockEntity> {
    private static final Component LINE_1 = Component.translatable("block.skofcraft.big_red_button.warning_1").withStyle(Style.EMPTY.withBold(true));
    private static final Component LINE_2 = Component.translatable("block.skofcraft.big_red_button.warning_2").withStyle(Style.EMPTY.withBold(true));
    private static final float SCALE_1 = 0.045F;
    private static final float SCALE_2 = 0.025F;

    private final Font font;

    public BigRedButtonRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(BigRedButtonBlockEntity button, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        float time = (button.getLevel() == null ? 0 : button.getLevel().getGameTime()) + partialTick;
        // Blinks between bright and dark red, and bobs a little
        float pulse = 0.5F + 0.5F * Mth.sin(time * 0.35F);
        int red = (int) Mth.lerp(pulse, 150, 255);
        int color = 0xFF000000 | red << 16 | (int) (pulse * 40) << 8 | (int) (pulse * 40);

        poseStack.pushPose();
        poseStack.translate(0.5D, 1.9D + Mth.sin(time * 0.08F) * 0.05D, 0.5D);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        drawLine(poseStack, buffers, LINE_1, SCALE_1, 0.0F, color);
        drawLine(poseStack, buffers, LINE_2, SCALE_2, -0.45F, color);
        poseStack.popPose();
    }

    private void drawLine(PoseStack poseStack, MultiBufferSource buffers, Component text, float scale, float yOffset, int color) {
        poseStack.pushPose();
        poseStack.translate(0.0D, yOffset, 0.0D);
        poseStack.scale(-scale, -scale, scale);
        Matrix4f matrix = poseStack.last().pose();
        float x = -font.width(text) / 2.0F;
        int background = 0xC0000000;
        font.drawInBatch(text, x, 0.0F, color, false, matrix, buffers, Font.DisplayMode.NORMAL, background, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(BigRedButtonBlockEntity button) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 48;
    }
}
