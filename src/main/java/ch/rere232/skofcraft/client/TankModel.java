package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.entity.Tank;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.util.Mth;

/**
 * A tank model with a "turret" part holding a "gun" part, at one block to the metre once {@link TankRenderer} has scaled
 * it. The nose points to -Z, the tracks sit on y = 24, and each box takes its texture from a large swatch of one material.
 */
public abstract class TankModel extends EntityModel<Tank> {
    private final ModelPart root;
    private final ModelPart turret;
    private final ModelPart gun;

    protected TankModel(ModelPart root) {
        this.root = root;
        this.turret = root.getChild("turret");
        this.gun = turret.getChild("gun");
    }

    protected static CubeListBuilder box(int[] swatch, float x, float y, float z, float w, float h, float d) {
        return CubeListBuilder.create().texOffs(swatch[0], swatch[1]).addBox(x, y, z, w, h, d);
    }

    @Override
    public void setupAnim(Tank tank, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float partial = ageInTicks - tank.tickCount;
        float yaw = Mth.rotLerp(partial, tank.turretYawO, tank.turretYaw);
        turret.yRot = (float) Math.toRadians(Mth.wrapDegrees(yaw - tank.getViewYRot(partial)));
        gun.xRot = (float) Math.toRadians(Mth.lerp(partial, tank.gunPitchO, tank.gunPitch));
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
                               float red, float green, float blue, float alpha) {
        root.render(poseStack, buffer, light, overlay, red, green, blue, alpha);
    }
}
