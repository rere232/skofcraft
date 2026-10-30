package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.F14Tomcat;
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

/** F-14 Tomcat with variable-sweep wings and retractable gear. The nose points to -Z and the wheels sit on y = 24. */
public class F14Model extends EntityModel<F14Tomcat> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "f14"), "main");

    private static final float MAX_SWEEP = 1.1F;

    private final ModelPart root;
    private final ModelPart wingLeft;
    private final ModelPart wingRight;
    private final ModelPart gear;

    public F14Model(ModelPart root) {
        this.root = root;
        this.wingLeft = root.getChild("wing_left");
        this.wingRight = root.getChild("wing_right");
        this.gear = root.getChild("gear");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        parts.addOrReplaceChild("fuselage", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5.0F, 10.0F, -12.0F, 10.0F, 6.0F, 34.0F), PartPose.ZERO);
        parts.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(90, 0)
                .addBox(-3.0F, 11.0F, -26.0F, 6.0F, 4.0F, 14.0F), PartPose.ZERO);
        parts.addOrReplaceChild("nose_tip", CubeListBuilder.create().texOffs(132, 0)
                .addBox(-1.5F, 11.5F, -32.0F, 3.0F, 3.0F, 6.0F), PartPose.ZERO);
        parts.addOrReplaceChild("canopy", CubeListBuilder.create().texOffs(0, 42)
                .addBox(-3.0F, 7.0F, -14.0F, 6.0F, 3.0F, 14.0F), PartPose.ZERO);
        parts.addOrReplaceChild("intake_left", CubeListBuilder.create().texOffs(42, 42)
                .addBox(5.0F, 11.0F, -2.0F, 4.0F, 5.0F, 18.0F), PartPose.ZERO);
        parts.addOrReplaceChild("intake_right", CubeListBuilder.create().texOffs(88, 42)
                .addBox(-9.0F, 11.0F, -2.0F, 4.0F, 5.0F, 18.0F), PartPose.ZERO);
        parts.addOrReplaceChild("exhaust_left", CubeListBuilder.create().texOffs(134, 42)
                .addBox(1.5F, 10.5F, 22.0F, 5.0F, 5.0F, 8.0F), PartPose.ZERO);
        parts.addOrReplaceChild("exhaust_right", CubeListBuilder.create().texOffs(162, 42)
                .addBox(-6.5F, 10.5F, 22.0F, 5.0F, 5.0F, 8.0F), PartPose.ZERO);
        parts.addOrReplaceChild("fin_left", CubeListBuilder.create().texOffs(0, 66)
                .addBox(5.5F, 2.0F, 20.0F, 1.0F, 9.0F, 8.0F), PartPose.ZERO);
        parts.addOrReplaceChild("fin_right", CubeListBuilder.create().texOffs(20, 66)
                .addBox(-6.5F, 2.0F, 20.0F, 1.0F, 9.0F, 8.0F), PartPose.ZERO);
        parts.addOrReplaceChild("tailplane_left", CubeListBuilder.create().texOffs(40, 66)
                .addBox(6.5F, 13.0F, 22.0F, 8.0F, 1.0F, 7.0F), PartPose.ZERO);
        parts.addOrReplaceChild("tailplane_right", CubeListBuilder.create().texOffs(72, 66)
                .addBox(-14.5F, 13.0F, 22.0F, 8.0F, 1.0F, 7.0F), PartPose.ZERO);

        parts.addOrReplaceChild("wing_left", CubeListBuilder.create().texOffs(0, 86)
                .addBox(0.0F, -0.5F, -6.0F, 22.0F, 1.0F, 12.0F), PartPose.offset(5.0F, 13.5F, 4.0F));
        parts.addOrReplaceChild("wing_right", CubeListBuilder.create().texOffs(70, 86)
                .addBox(-22.0F, -0.5F, -6.0F, 22.0F, 1.0F, 12.0F), PartPose.offset(-5.0F, 13.5F, 4.0F));

        PartDefinition gear = parts.addOrReplaceChild("gear", CubeListBuilder.create(), PartPose.ZERO);
        gear.addOrReplaceChild("nose_strut", CubeListBuilder.create().texOffs(0, 108)
                .addBox(-0.5F, 16.0F, -18.0F, 1.0F, 7.0F, 1.0F), PartPose.ZERO);
        gear.addOrReplaceChild("nose_wheel", CubeListBuilder.create().texOffs(14, 108)
                .addBox(-1.0F, 21.0F, -18.5F, 2.0F, 3.0F, 3.0F), PartPose.ZERO);
        gear.addOrReplaceChild("left_strut", CubeListBuilder.create().texOffs(0, 108)
                .addBox(5.5F, 16.0F, 10.0F, 1.0F, 7.0F, 1.0F), PartPose.ZERO);
        gear.addOrReplaceChild("left_wheel", CubeListBuilder.create().texOffs(14, 108)
                .addBox(5.0F, 21.0F, 9.5F, 2.0F, 3.0F, 3.0F), PartPose.ZERO);
        gear.addOrReplaceChild("right_strut", CubeListBuilder.create().texOffs(0, 108)
                .addBox(-6.5F, 16.0F, 10.0F, 1.0F, 7.0F, 1.0F), PartPose.ZERO);
        gear.addOrReplaceChild("right_wheel", CubeListBuilder.create().texOffs(14, 108)
                .addBox(-7.0F, 21.0F, 9.5F, 2.0F, 3.0F, 3.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(F14Tomcat jet, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float partial = ageInTicks - jet.tickCount;
        float sweep = Mth.lerp(partial, jet.wingSweepO, jet.wingSweep) * MAX_SWEEP;
        wingLeft.yRot = -sweep;
        wingRight.yRot = sweep;
        gear.visible = jet.onGround();
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
                               float red, float green, float blue, float alpha) {
        root.render(poseStack, buffer, light, overlay, red, green, blue, alpha);
    }
}
