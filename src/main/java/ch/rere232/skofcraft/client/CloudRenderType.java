package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Soft smoke puffs for the mushroom cloud: the particle shader and format, translucent, no culling, and no depth
 * writes, so overlapping puffs blend into each other instead of cutting hard edges. Quads are sorted on upload.
 */
public final class CloudRenderType extends RenderType {
    public static final ResourceLocation PUFF_TEXTURE = ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "textures/misc/cloud_puff.png");

    public static final RenderType PUFFS = create("skofcraft_cloud_puffs", DefaultVertexFormat.PARTICLE, VertexFormat.Mode.QUADS,
            262144, false, true, CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getParticleShader))
                    .setTextureState(new TextureStateShard(PUFF_TEXTURE, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private CloudRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean crumbling,
                            boolean sort, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, crumbling, sort, setup, clear);
    }
}
