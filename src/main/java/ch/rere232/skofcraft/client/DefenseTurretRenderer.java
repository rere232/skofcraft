package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.block.DefenseTurretBlock;
import ch.rere232.skofcraft.blockentity.DefenseTurretBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Draws the turret head on its pedestal, aimed where the block entity says. */
public class DefenseTurretRenderer implements BlockEntityRenderer<DefenseTurretBlockEntity> {
    private static final ResourceLocation CIWS_TEXTURE = ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "textures/entity/ciws.png");
    private static final ResourceLocation SAM_TEXTURE = ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "textures/entity/sam_launcher.png");
    /** Top of the pedestal block model. */
    private static final double PEDESTAL_TOP = 10.0D / 16.0D;

    private final DefenseTurretModel ciws;
    private final DefenseTurretModel sam;

    public DefenseTurretRenderer(BlockEntityRendererProvider.Context context) {
        this.ciws = new DefenseTurretModel(context.bakeLayer(DefenseTurretModel.CIWS_LAYER));
        this.sam = new DefenseTurretModel(context.bakeLayer(DefenseTurretModel.SAM_LAYER));
    }

    @Override
    public void render(DefenseTurretBlockEntity turret, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        boolean isSam = turret.weapon() == DefenseTurretBlock.Weapon.SAM;
        DefenseTurretModel model = isSam ? sam : ciws;
        model.setup(Mth.rotLerp(partialTick, turret.prevYaw, turret.yaw),
                Mth.lerp(partialTick, turret.prevPitch, turret.pitch),
                Mth.lerp(partialTick, turret.prevSpin, turret.spin));
        // The head sticks up out of the block, so light it from the space above
        int light = turret.getLevel() == null ? packedLight : LevelRenderer.getLightColor(turret.getLevel(), turret.getBlockPos().above());
        poseStack.pushPose();
        poseStack.translate(0.5D, PEDESTAL_TOP, 0.5D);
        model.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(isSam ? SAM_TEXTURE : CIWS_TEXTURE)), light, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
