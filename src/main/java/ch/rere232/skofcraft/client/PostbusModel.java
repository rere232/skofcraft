package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.Postbus;
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
import net.minecraft.util.Mth;

/** Postal bus model. The nose points to -Z and the wheels sit on y = 24. */
public class PostbusModel extends EntityModel<Postbus> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "postbus"), "main");

    private final ModelPart root;
    private final ModelPart frontLeft;
    private final ModelPart frontRight;
    private final ModelPart rearLeft;
    private final ModelPart rearRight;

    public PostbusModel(ModelPart root) {
        this.root = root;
        this.frontLeft = root.getChild("wheel_front_left");
        this.frontRight = root.getChild("wheel_front_right");
        this.rearLeft = root.getChild("wheel_rear_left");
        this.rearRight = root.getChild("wheel_rear_right");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        parts.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-11.0F, 1.0F, -36.0F, 22.0F, 19.0F, 72.0F), PartPose.ZERO);
        parts.addOrReplaceChild("air_conditioning", CubeListBuilder.create().texOffs(24, 92)
                .addBox(-6.0F, -1.0F, -6.0F, 12.0F, 2.0F, 20.0F), PartPose.ZERO);
        parts.addOrReplaceChild("mirror_left", CubeListBuilder.create().texOffs(92, 92)
                .addBox(11.0F, 4.0F, -35.0F, 2.0F, 4.0F, 1.0F), PartPose.ZERO);
        parts.addOrReplaceChild("mirror_right", CubeListBuilder.create().texOffs(92, 92)
                .addBox(-13.0F, 4.0F, -35.0F, 2.0F, 4.0F, 1.0F), PartPose.ZERO);
        parts.addOrReplaceChild("bumper", CubeListBuilder.create().texOffs(104, 92)
                .addBox(-11.0F, 17.0F, -37.0F, 22.0F, 3.0F, 1.0F), PartPose.ZERO);
        // The three bells of the horn, hanging under the front like on the real thing
        for (int i = 0; i < 3; i++) {
            parts.addOrReplaceChild("horn_" + i, CubeListBuilder.create().texOffs(154, 92)
                    .addBox(-4.5F + i * 4.0F, 20.0F, -35.0F, 1.0F, 1.0F, 3.0F), PartPose.ZERO);
        }
        wheel(parts, "wheel_front_left", 10.5F, -24.0F);
        wheel(parts, "wheel_front_right", -10.5F, -24.0F);
        wheel(parts, "wheel_rear_left", 10.5F, 22.0F);
        wheel(parts, "wheel_rear_right", -10.5F, 22.0F);
        return LayerDefinition.create(mesh, 256, 128);
    }

    private static void wheel(PartDefinition parts, String name, float x, float z) {
        parts.addOrReplaceChild(name, CubeListBuilder.create().texOffs(0, 92)
                .addBox(-1.5F, -3.0F, -3.0F, 3.0F, 6.0F, 6.0F), PartPose.offset(x, 21.0F, z));
    }

    @Override
    public void setupAnim(Postbus bus, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float partial = ageInTicks - bus.tickCount;
        float angle = Mth.lerp(partial, bus.wheelAngleO, bus.wheelAngle);
        float steer = Mth.lerp(partial, bus.steerO, bus.steer);
        frontLeft.xRot = angle;
        frontRight.xRot = angle;
        rearLeft.xRot = angle;
        rearRight.xRot = angle;
        frontLeft.yRot = steer;
        frontRight.yRot = steer;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
                               float red, float green, float blue, float alpha) {
        root.render(poseStack, buffer, light, overlay, red, green, blue, alpha);
    }
}
