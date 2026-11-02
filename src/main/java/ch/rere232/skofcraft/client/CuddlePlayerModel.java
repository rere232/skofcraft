package ch.rere232.skofcraft.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

/**
 * The player model, with the cuddle pose on top of vanilla's animation. Armor, held items and Curios all copy their
 * pose from this model, so they follow it. Model space: +X is the player's left, +Y is down and -Z is forward.
 */
public class CuddlePlayerModel extends PlayerModel<AbstractClientPlayer> {
    // Over the partner's shoulders, reaching behind their neck
    private static final float OVER_ARM_PITCH = -1.85F;
    private static final float OVER_ARM_INWARD = 0.28F;
    // Around the partner's waist, with the hands closing in on their back
    private static final float UNDER_ARM_PITCH = -1.0F;
    private static final float UNDER_ARM_INWARD = 0.14F;
    // Cheek to cheek: both heads go to their owner's left, so they pass each other
    private static final float HEAD_PITCH = 0.12F;
    private static final float HEAD_TURN = -0.25F;
    private static final float HEAD_TILT = 0.25F;
    private static final float HEAD_SIDEWAYS = 1.2F;
    private static final float HEAD_BACK = 0.8F;
    private static final float FOOT_POP = 0.3F;

    public CuddlePlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Vanilla never moves the head sideways, so it never puts it back, and this model is shared by every player
        head.x = 0.0F;
        head.z = 0.0F;
        super.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        CuddleClient.Pose pose = CuddleClient.currentPose();
        if (pose == null || pose.player() != player) {
            return;
        }
        float blend = pose.blend();
        float time = pose.time();
        float breath = Mth.sin(time * 0.08F) * 0.02F;
        // Every few seconds, the one on top pats their partner on the back
        float pat = Mth.sin(time * 0.06F) > 0.7F ? Mth.sin(time * 1.1F) * 0.07F : 0.0F;

        float pitch = (pose.over() ? OVER_ARM_PITCH : UNDER_ARM_PITCH) + breath;
        float inward = pose.over() ? OVER_ARM_INWARD : UNDER_ARM_INWARD;
        apply(rightArm, blend, pitch + (pose.over() ? pat : 0.0F), -inward, 0.0F);
        apply(leftArm, blend, pitch, inward, 0.0F);
        apply(head, blend, HEAD_PITCH + breath, HEAD_TURN, HEAD_TILT);
        head.x = HEAD_SIDEWAYS * blend;
        head.z = HEAD_BACK * blend;
        apply(rightLeg, blend, 0.0F, 0.0F, 0.0F);
        apply(leftLeg, blend, pose.over() ? 0.0F : FOOT_POP, 0.0F, 0.0F);

        // Vanilla copied these from the parts above before we changed them
        hat.copyFrom(head);
        leftSleeve.copyFrom(leftArm);
        rightSleeve.copyFrom(rightArm);
        leftPants.copyFrom(leftLeg);
        rightPants.copyFrom(rightLeg);
        jacket.copyFrom(body);
    }

    private static void apply(ModelPart part, float blend, float xRot, float yRot, float zRot) {
        part.xRot = Mth.lerp(blend, part.xRot, xRot);
        part.yRot = Mth.lerp(blend, part.yRot, yRot);
        part.zRot = Mth.lerp(blend, part.zRot, zRot);
    }
}
