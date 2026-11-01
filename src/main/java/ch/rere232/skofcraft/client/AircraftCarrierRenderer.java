package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.carrier.CarrierBlocks;
import ch.rere232.skofcraft.entity.AircraftCarrier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Draws a carrier underway from its block list. The blocks are tessellated once into a cached mesh (hidden faces
 * culled, ambient occlusion baked into the colours) and the mesh is replayed every frame with the entity's light.
 */
@OnlyIn(Dist.CLIENT)
public class AircraftCarrierRenderer extends EntityRenderer<AircraftCarrier> {
    public AircraftCarrierRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(AircraftCarrier carrier, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        Mesh mesh;
        if (carrier.renderCache instanceof Mesh cached) {
            mesh = cached;
        } else {
            mesh = Mesh.bake(carrier);
            carrier.renderCache = mesh;
        }
        pose.pushPose();
        float yaw = Mth.rotLerp(partialTick, carrier.yRotO, carrier.getYRot());
        pose.mulPose(Axis.YP.rotationDegrees(AircraftCarrier.frameRotation(yaw)));
        mesh.draw(pose, buffers, light);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(AircraftCarrier carrier) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    /** Vertices per chunk render type: x, y, z, r, g, b, a, u, v, nx, ny, nz. */
    private static final class Mesh {
        private static final int STRIDE = 12;
        private final Map<RenderType, Recorder> layers = new LinkedHashMap<>();

        static Mesh bake(AircraftCarrier carrier) {
            Mesh mesh = new Mesh();
            CarrierBlocks blocks = carrier.getBlocks();
            ShipView view = new ShipView(blocks, carrier.blockPosition());
            BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
            RandomSource random = RandomSource.create();
            PoseStack pose = new PoseStack();
            for (int i = 0; i < blocks.size(); i++) {
                BlockState state = blocks.state(i);
                if (state.getRenderShape() != RenderShape.MODEL) {
                    continue;
                }
                BlockPos local = blocks.localPos(i);
                BakedModel model = dispatcher.getBlockModel(state);
                long seed = state.getSeed(local);
                random.setSeed(seed);
                for (RenderType type : model.getRenderTypes(state, random, ModelData.EMPTY)) {
                    pose.pushPose();
                    // Block cells are centred on the ship's frame so rotating about the entity lines up with BlockPos.rotate
                    pose.translate(local.getX() - 0.5D, local.getY(), local.getZ() - 0.5D);
                    dispatcher.getModelRenderer().tesselateBlock(view, model, state, local, pose,
                            mesh.layers.computeIfAbsent(type, t -> new Recorder()), true, random, seed,
                            OverlayTexture.NO_OVERLAY, ModelData.EMPTY, type);
                    pose.popPose();
                }
            }
            return mesh;
        }

        void draw(PoseStack pose, MultiBufferSource buffers, int light) {
            Matrix4f matrix = pose.last().pose();
            Matrix3f normal = pose.last().normal();
            layers.forEach((type, recorder) -> {
                VertexConsumer consumer = buffers.getBuffer(RenderTypeHelper.getMovingBlockRenderType(type));
                float[] v = recorder.data;
                for (int i = 0; i < recorder.size; i += STRIDE) {
                    consumer.vertex(matrix, v[i], v[i + 1], v[i + 2])
                            .color(v[i + 3], v[i + 4], v[i + 5], v[i + 6])
                            .uv(v[i + 7], v[i + 8])
                            .overlayCoords(OverlayTexture.NO_OVERLAY)
                            .uv2(light)
                            .normal(normal, v[i + 9], v[i + 10], v[i + 11])
                            .endVertex();
                }
            });
        }
    }

    /** Collects tessellated vertices; light and overlay are dropped and supplied again at draw time. */
    private static final class Recorder implements VertexConsumer {
        private float[] data = new float[4096];
        private int size;
        private final float[] current = new float[Mesh.STRIDE];
        private boolean defaultColor;
        private final float[] fixedColor = new float[4];

        private void ensure() {
            if (size + Mesh.STRIDE > data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            current[0] = (float) x;
            current[1] = (float) y;
            current[2] = (float) z;
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            current[3] = r / 255.0F;
            current[4] = g / 255.0F;
            current[5] = b / 255.0F;
            current[6] = a / 255.0F;
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            current[7] = u;
            current[8] = v;
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            current[9] = x;
            current[10] = y;
            current[11] = z;
            return this;
        }

        @Override
        public void endVertex() {
            if (defaultColor) {
                System.arraycopy(fixedColor, 0, current, 3, 4);
            }
            ensure();
            System.arraycopy(current, 0, data, size, Mesh.STRIDE);
            size += Mesh.STRIDE;
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
            defaultColor = true;
            fixedColor[0] = r / 255.0F;
            fixedColor[1] = g / 255.0F;
            fixedColor[2] = b / 255.0F;
            fixedColor[3] = a / 255.0F;
        }

        @Override
        public void unsetDefaultColor() {
            defaultColor = false;
        }
    }

    /** The ship's blocks as a tiny world, so the block renderer can cull faces between them and shade corners. */
    private static final class ShipView implements BlockAndTintGetter {
        private final Map<BlockPos, BlockState> states = new HashMap<>();
        private final BlockPos anchor;

        ShipView(CarrierBlocks blocks, BlockPos anchor) {
            this.anchor = anchor;
            for (int i = 0; i < blocks.size(); i++) {
                states.put(blocks.localPos(i), blocks.state(i));
            }
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return states.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Nullable
        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public float getShade(Direction direction, boolean shade) {
            return Minecraft.getInstance().level.getShade(direction, shade);
        }

        @Override
        public LevelLightEngine getLightEngine() {
            return Minecraft.getInstance().level.getLightEngine();
        }

        @Override
        public int getBrightness(LightLayer layer, BlockPos pos) {
            return layer == LightLayer.SKY ? 15 : 0;
        }

        @Override
        public int getRawBrightness(BlockPos pos, int darkening) {
            return 15;
        }

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver resolver) {
            return Minecraft.getInstance().level.getBlockTint(anchor, resolver);
        }

        @Override
        public int getHeight() {
            return 512;
        }

        @Override
        public int getMinBuildHeight() {
            return -256;
        }
    }
}
