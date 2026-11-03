package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import org.joml.Vector3f;

/** Textures and the view from inside liquid SKOFee, which is mostly brown. */
public class SkofeeFluidClient implements IClientFluidTypeExtensions {
    private static final ResourceLocation STILL = ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "block/skofee_still");
    private static final ResourceLocation FLOWING = ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "block/skofee_flow");
    private static final float VISIBILITY = 4.0F;

    @Override
    public ResourceLocation getStillTexture() {
        return STILL;
    }

    @Override
    public ResourceLocation getFlowingTexture() {
        return FLOWING;
    }

    @Override
    public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
        return new Vector3f(0.20F, 0.11F, 0.05F);
    }

    @Override
    public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance, float partialTick, float nearDistance, float farDistance, FogShape shape) {
        RenderSystem.setShaderFogStart(0.0F);
        RenderSystem.setShaderFogEnd(VISIBILITY);
    }
}
