package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.ApacheHelicopter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/** Attack helicopter model. The nose points to -Z and the skids sit on y = 24. */
public class ApacheModel extends EntityModel<ApacheHelicopter> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "apache"), "main");

    private final ModelPart root;
    private final ModelPart rotor;
    private final ModelPart tailRotor;

    public ApacheModel(ModelPart root) {
        this.root = root;
        this.rotor = root.getChild("rotor");
        this.tailRotor = root.getChild("tail_rotor");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        parts.addOrReplaceChild("fuselage", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.0F, 8.0F, -14.0F, 6.0F, 9.0F, 28.0F), PartPose.ZERO);
        parts.addOrReplaceChild("chin", CubeListBuilder.create().texOffs(70, 0)
                .addBox(-2.5F, 11.0F, -20.0F, 5.0F, 5.0F, 7.0F), PartPose.ZERO);
        parts.addOrReplaceChild("gun", CubeListBuilder.create().texOffs(70, 94)
                .addBox(-0.5F, 14.5F, -26.0F, 1.0F, 1.0F, 6.0F), PartPose.ZERO);
        parts.addOrReplaceChild("canopy", CubeListBuilder.create().texOffs(0, 38)
                .addBox(-2.5F, 4.0F, -12.0F, 5.0F, 4.0F, 12.0F), PartPose.ZERO);
        parts.addOrReplaceChild("nacelle_left", CubeListBuilder.create().texOffs(36, 38)
                .addBox(3.0F, 5.0F, 0.0F, 3.0F, 4.0F, 10.0F), PartPose.ZERO);
        parts.addOrReplaceChild("nacelle_right", CubeListBuilder.create().texOffs(64, 38)
                .addBox(-6.0F, 5.0F, 0.0F, 3.0F, 4.0F, 10.0F), PartPose.ZERO);
        parts.addOrReplaceChild("tail_boom", CubeListBuilder.create().texOffs(0, 56)
                .addBox(-1.5F, 9.0F, 14.0F, 3.0F, 3.0F, 22.0F), PartPose.ZERO);
        parts.addOrReplaceChild("tail_fin", CubeListBuilder.create().texOffs(52, 56)
                .addBox(-0.5F, 2.0F, 30.0F, 1.0F, 9.0F, 7.0F), PartPose.ZERO);
        parts.addOrReplaceChild("stabilizer", CubeListBuilder.create().texOffs(70, 56)
                .addBox(-7.0F, 9.5F, 31.0F, 14.0F, 1.0F, 5.0F), PartPose.ZERO);
        parts.addOrReplaceChild("wings", CubeListBuilder.create().texOffs(0, 84)
                .addBox(-14.0F, 13.0F, -4.0F, 28.0F, 2.0F, 7.0F), PartPose.ZERO);
        parts.addOrReplaceChild("pod_left", CubeListBuilder.create().texOffs(72, 84)
                .addBox(10.0F, 15.0F, -5.0F, 3.0F, 3.0F, 9.0F), PartPose.ZERO);
        parts.addOrReplaceChild("pod_right", CubeListBuilder.create().texOffs(98, 84)
                .addBox(-13.0F, 15.0F, -5.0F, 3.0F, 3.0F, 9.0F), PartPose.ZERO);
        parts.addOrReplaceChild("skid_left", CubeListBuilder.create().texOffs(0, 94)
                .addBox(4.0F, 23.0F, -12.0F, 1.0F, 1.0F, 22.0F), PartPose.ZERO);
        parts.addOrReplaceChild("skid_right", CubeListBuilder.create().texOffs(0, 94)
                .addBox(-5.0F, 23.0F, -12.0F, 1.0F, 1.0F, 22.0F), PartPose.ZERO);
        for (int i = 0; i < 4; i++) {
            float x = i % 2 == 0 ? 4.0F : -5.0F;
            float z = i < 2 ? -8.0F : 6.0F;
            parts.addOrReplaceChild("strut_" + i, CubeListBuilder.create().texOffs(48, 94)
                    .addBox(x, 17.0F, z, 1.0F, 6.0F, 1.0F), PartPose.ZERO);
        }
        parts.addOrReplaceChild("mast", CubeListBuilder.create().texOffs(54, 94)
                .addBox(-1.0F, 1.0F, -1.0F, 2.0F, 4.0F, 2.0F), PartPose.ZERO);

        PartDefinition rotor = parts.addOrReplaceChild("rotor", CubeListBuilder.create().texOffs(0, 118)
                .addBox(-28.0F, -1.0F, -1.5F, 56.0F, 1.0F, 3.0F), PartPose.offset(0.0F, 1.0F, 0.0F));
        rotor.addOrReplaceChild("blades_cross", CubeListBuilder.create().texOffs(0, 118)
                .addBox(-28.0F, -0.8F, -1.5F, 56.0F, 1.0F, 3.0F), PartPose.rotation(0.0F, (float) Math.PI / 2.0F, 0.0F));

        PartDefinition tail = parts.addOrReplaceChild("tail_rotor", CubeListBuilder.create().texOffs(64, 94)
                .addBox(0.0F, -4.5F, -0.5F, 1.0F, 9.0F, 1.0F), PartPose.offset(2.0F, 8.0F, 34.0F));
        tail.addOrReplaceChild("tail_cross", CubeListBuilder.create().texOffs(64, 94)
                .addBox(0.0F, -4.5F, -0.5F, 1.0F, 9.0F, 1.0F), PartPose.rotation((float) Math.PI / 2.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(ApacheHelicopter helicopter, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float partial = ageInTicks - helicopter.tickCount;
        float angle = net.minecraft.util.Mth.lerp(partial, helicopter.rotorAngleO, helicopter.rotorAngle);
        rotor.yRot = (float) Math.toRadians(angle);
        tailRotor.xRot = (float) Math.toRadians(angle * 1.4F);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
                               float red, float green, float blue, float alpha) {
        root.render(poseStack, buffer, light, overlay, red, green, blue, alpha);
    }
}
