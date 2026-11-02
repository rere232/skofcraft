package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.FpvDrone;
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

/** An X-frame racing quad with a warhead strapped under it. The camera points to -Z and the warhead sits on y = 24. */
public class FpvDroneModel extends EntityModel<FpvDrone> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "fpv_drone"), "main");

    private static final float MOTOR_Y = 18.0F;
    private static final float[][] MOTORS = {{-5.0F, -5.0F}, {5.0F, -5.0F}, {-5.0F, 5.0F}, {5.0F, 5.0F}};

    private final ModelPart root;
    private final ModelPart[] props = new ModelPart[MOTORS.length];

    public FpvDroneModel(ModelPart root) {
        this.root = root;
        for (int i = 0; i < props.length; i++) {
            props[i] = root.getChild("prop_" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        // Swatches on the sheet: carbon (0,0), red tape (32,0), olive warhead (0,16), yellow battery (32,16), metal (48,16), black (48,0)
        parts.addOrReplaceChild("frame", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2.0F, 19.0F, -3.0F, 4.0F, 1.0F, 6.0F), PartPose.ZERO);
        parts.addOrReplaceChild("battery", CubeListBuilder.create().texOffs(32, 16)
                .addBox(-1.5F, 17.5F, -1.5F, 3.0F, 1.5F, 4.0F), PartPose.ZERO);
        parts.addOrReplaceChild("warhead", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-1.5F, 20.0F, -5.0F, 3.0F, 3.0F, 9.0F), PartPose.ZERO);
        parts.addOrReplaceChild("tape", CubeListBuilder.create().texOffs(32, 0)
                .addBox(-1.75F, 19.75F, -1.0F, 3.5F, 3.5F, 1.0F)
                .addBox(-1.75F, 19.75F, 2.0F, 3.5F, 3.5F, 1.0F), PartPose.ZERO);
        parts.addOrReplaceChild("fuze", CubeListBuilder.create().texOffs(48, 16)
                .addBox(-0.5F, 21.0F, -7.0F, 1.0F, 1.0F, 2.0F), PartPose.ZERO);
        parts.addOrReplaceChild("camera", CubeListBuilder.create().texOffs(48, 0)
                .addBox(-1.0F, 17.5F, -3.5F, 2.0F, 1.5F, 1.5F), PartPose.ZERO);
        parts.addOrReplaceChild("antenna", CubeListBuilder.create().texOffs(48, 0)
                .addBox(-0.25F, 15.0F, 3.0F, 0.5F, 4.0F, 0.5F), PartPose.ZERO);
        for (int i = 0; i < MOTORS.length; i++) {
            float x = MOTORS[i][0];
            float z = MOTORS[i][1];
            float length = Mth.sqrt(x * x + z * z);
            parts.addOrReplaceChild("arm_" + i, CubeListBuilder.create().texOffs(0, 0)
                            .addBox(-0.5F, 0.0F, 0.0F, 1.0F, 0.5F, length),
                    PartPose.offsetAndRotation(0.0F, 19.25F, 0.0F, 0.0F, (float) Math.atan2(x, z), 0.0F));
            parts.addOrReplaceChild("motor_" + i, CubeListBuilder.create().texOffs(48, 16)
                    .addBox(x - 0.75F, MOTOR_Y + 0.25F, z - 0.75F, 1.5F, 1.5F, 1.5F), PartPose.ZERO);
            parts.addOrReplaceChild("prop_" + i, CubeListBuilder.create().texOffs(48, 0)
                            .addBox(-3.0F, 0.0F, -0.4F, 6.0F, 0.1F, 0.8F)
                            .addBox(-0.4F, 0.0F, -3.0F, 0.8F, 0.1F, 6.0F),
                    PartPose.offset(x, MOTOR_Y, z));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    public void setProps(float angle) {
        for (int i = 0; i < props.length; i++) {
            // Diagonal pairs turn the same way, as they have to for it not to spin in the air
            boolean clockwise = i == 0 || i == 3;
            props[i].yRot = (clockwise ? angle : -angle) * Mth.DEG_TO_RAD;
        }
    }

    @Override
    public void setupAnim(FpvDrone drone, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, float red, float green, float blue, float alpha) {
        root.render(poseStack, consumer, light, overlay, red, green, blue, alpha);
    }
}
