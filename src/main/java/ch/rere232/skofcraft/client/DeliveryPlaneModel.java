package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.DeliveryPlane;
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

/** Small propeller plane trailing an Amazooon banner. The nose points to -Z. */
public class DeliveryPlaneModel extends EntityModel<DeliveryPlane> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "delivery_plane"), "main");

    private final ModelPart root;
    private final ModelPart propeller;

    public DeliveryPlaneModel(ModelPart root) {
        this.root = root;
        this.propeller = root.getChild("propeller");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        parts.addOrReplaceChild("fuselage", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.0F, 10.0F, -12.0F, 6.0F, 6.0F, 24.0F), PartPose.ZERO);
        parts.addOrReplaceChild("wings", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-22.0F, 12.0F, -4.0F, 44.0F, 2.0F, 9.0F), PartPose.ZERO);
        parts.addOrReplaceChild("tail_wing", CubeListBuilder.create().texOffs(62, 0)
                .addBox(-8.0F, 12.0F, 8.0F, 16.0F, 1.0F, 5.0F), PartPose.ZERO);
        parts.addOrReplaceChild("tail_fin", CubeListBuilder.create().texOffs(62, 8)
                .addBox(-0.5F, 5.0F, 8.0F, 1.0F, 7.0F, 6.0F), PartPose.ZERO);
        parts.addOrReplaceChild("propeller", CubeListBuilder.create().texOffs(84, 8)
                .addBox(-10.0F, -1.0F, -0.5F, 20.0F, 2.0F, 1.0F), PartPose.offset(0.0F, 13.0F, -12.5F));
        parts.addOrReplaceChild("banner", CubeListBuilder.create().texOffs(0, 64)
                .addBox(-0.5F, 10.0F, 14.0F, 1.0F, 8.0F, 38.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(DeliveryPlane entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        propeller.zRot = ageInTicks * 1.6F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
                               float red, float green, float blue, float alpha) {
        root.render(poseStack, buffer, light, overlay, red, green, blue, alpha);
    }
}
