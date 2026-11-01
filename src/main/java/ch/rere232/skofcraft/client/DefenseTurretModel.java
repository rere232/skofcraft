package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Turret heads, drawn on top of the pedestal block model. Built y-up with the guns along +Z:
 * yaw -> pitch -> spin (the gatling's barrel cluster; empty on the missile launcher).
 */
public class DefenseTurretModel {
    public static final ModelLayerLocation CIWS_LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "ciws"), "main");
    public static final ModelLayerLocation SAM_LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "sam_launcher"), "main");
    /** Pitch pivot height above the pedestal top, in pixels; with the 10 px pedestal this is DefenseTurretBlockEntity.PIVOT_HEIGHT. */
    private static final float PIVOT = 6.0F;

    private final ModelPart root;
    private final ModelPart yaw;
    private final ModelPart pitch;
    private final ModelPart spin;

    public DefenseTurretModel(ModelPart root) {
        this.root = root;
        this.yaw = root.getChild("yaw");
        this.pitch = yaw.getChild("pitch");
        this.spin = pitch.getChild("spin");
    }

    public static LayerDefinition createCiwsLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition yaw = mesh.getRoot().addOrReplaceChild("yaw", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 2.0F, 10.0F), PartPose.ZERO);
        PartDefinition pitch = yaw.addOrReplaceChild("pitch", CubeListBuilder.create()
                .texOffs(0, 12).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                .texOffs(32, 12).addBox(-3.0F, 4.0F, -4.0F, 6.0F, 5.0F, 6.0F), PartPose.offset(0.0F, PIVOT, 0.0F));
        CubeListBuilder barrels = CubeListBuilder.create().texOffs(24, 28).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 2.0F);
        for (int i = 0; i < 6; i++) {
            float angle = i * Mth.TWO_PI / 6.0F;
            barrels.texOffs(0, 28).addBox(Mth.cos(angle) * 1.2F - 0.5F, Mth.sin(angle) * 1.2F - 0.5F, 2.0F, 1.0F, 1.0F, 10.0F);
        }
        pitch.addOrReplaceChild("spin", barrels, PartPose.offset(0.0F, -1.0F, 4.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createSamLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition yaw = mesh.getRoot().addOrReplaceChild("yaw", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6.0F, 0.0F, -6.0F, 12.0F, 2.0F, 12.0F)
                .texOffs(48, 0).addBox(-1.5F, 2.0F, -1.5F, 3.0F, 4.0F, 3.0F), PartPose.ZERO);
        CubeListBuilder pod = CubeListBuilder.create().texOffs(0, 14).addBox(-6.0F, -3.0F, -7.0F, 12.0F, 6.0F, 14.0F);
        for (float x : new float[] {-3.0F, 3.0F}) {
            for (float y : new float[] {-1.5F, 1.5F}) {
                pod.texOffs(0, 34).addBox(x - 0.5F, y - 0.5F, 7.0F, 1.0F, 1.0F, 2.0F);
            }
        }
        PartDefinition pitch = yaw.addOrReplaceChild("pitch", pod, PartPose.offset(0.0F, PIVOT, 0.0F));
        pitch.addOrReplaceChild("spin", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Angles in degrees, Minecraft convention: yaw 0 looks south, positive pitch looks down. */
    public void setup(float yawDegrees, float pitchDegrees, float spinDegrees) {
        yaw.yRot = -yawDegrees * Mth.DEG_TO_RAD;
        pitch.xRot = pitchDegrees * Mth.DEG_TO_RAD;
        spin.zRot = spinDegrees * Mth.DEG_TO_RAD;
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        root.render(poseStack, consumer, light, overlay);
    }
}
