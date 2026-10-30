package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.blockentity.ChichaBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.UUID;

/** Draws a sagging hose from the chicha to each player who holds one of its hoses. */
public class ChichaRenderer implements BlockEntityRenderer<ChichaBlockEntity> {
    private static final int SEGMENTS = 24;
    private static final float WIDTH = 0.03F;
    /** Hose ports on the vase, in the same order as the blockstate tube rotations: east, south, west, north. */
    private static final double[][] PORT_DIRECTIONS = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};

    public ChichaRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ChichaBlockEntity chicha, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (chicha.getLevel() == null) {
            return;
        }
        for (int i = 0; i < ChichaBlockEntity.TUBES; i++) {
            UUID user = chicha.getUser(i);
            if (user == null) {
                continue;
            }
            Player player = chicha.getLevel().getPlayerByUUID(user);
            if (player == null) {
                continue;
            }
            Vec3 from = new Vec3(0.5D + PORT_DIRECTIONS[i][0] * 0.31D, 0.55D, 0.5D + PORT_DIRECTIONS[i][1] * 0.31D);
            Vec3 mouth = player.getEyePosition(partialTick)
                    .add(player.getViewVector(partialTick).scale(0.3D))
                    .add(0.0D, -0.3D, 0.0D);
            Vec3 to = mouth.subtract(Vec3.atLowerCornerOf(chicha.getBlockPos()));
            drawHose(pose, buffers, from, to, light);
        }
    }

    private static void drawHose(PoseStack pose, MultiBufferSource buffers, Vec3 from, Vec3 to, int light) {
        float dx = (float) (to.x - from.x);
        float dy = (float) (to.y - from.y);
        float dz = (float) (to.z - from.z);
        float horizontal = (float) Math.sqrt(dx * dx + dz * dz);
        float ox = horizontal > 1.0E-4F ? dz / horizontal * WIDTH : WIDTH;
        float oz = horizontal > 1.0E-4F ? -dx / horizontal * WIDTH : 0.0F;
        float sag = Math.min(0.6F, 0.15F + horizontal * 0.12F);

        pose.pushPose();
        pose.translate(from.x, from.y, from.z);
        Matrix4f matrix = pose.last().pose();
        VertexConsumer consumer = buffers.getBuffer(RenderType.leash());
        for (int i = 0; i <= SEGMENTS; i++) {
            float t = i / (float) SEGMENTS;
            float x = dx * t;
            float y = dy * t - sag * 4.0F * t * (1.0F - t);
            float z = dz * t;
            float shade = i % 2 == 0 ? 0.85F : 1.0F;
            consumer.vertex(matrix, x - ox, y + WIDTH, z - oz).color(0.66F * shade, 0.16F * shade, 0.2F * shade, 1.0F).uv2(light).endVertex();
            consumer.vertex(matrix, x + ox, y - WIDTH, z + oz).color(0.66F * shade, 0.16F * shade, 0.2F * shade, 1.0F).uv2(light).endVertex();
        }
        pose.popPose();
    }
}
