package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.item.MaskItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

import java.util.Locale;

/**
 * Draws a piece of clothing on whoever wears it in a Curios slot. Each piece's parts are built in the space of the
 * humanoid part they follow (the body for the choker, crop top and skirt, the legs for the thigh highs, the arms for
 * the hand warmers, the head for the face mask and cat ears, the body for the tail) and copy that part's pose every frame, so they move, sneak and swim with the wearer.
 */
public class ClothingRenderer implements ICurioRenderer {
    public enum Piece {
        CHOKER(32, 16),
        CROP_TOP(64, 32),
        SKIRT(64, 32),
        THIGH_HIGHS(32, 32),
        HAND_WARMERS(64, 32),
        // Drawn at four times this resolution, so the printed faces can have some detail
        MASK(32, 16),
        TACTICAL_GLOVES(64, 16),
        CAT_EARS(32, 16),
        CAT_TAIL(32, 16);

        /** How far the tail bends at each joint, from the base up to the curled tip. */
        private static final float[] TAIL_BENDS = {2.0F, 0.4F, 0.4F, 0.5F};
        private static final float TAIL_SEGMENT = 3.0F;

        /** The open heart ring on the choker, drawn in half-pixel cells. */
        private static final String[] HEART = {
                ".##.##.",
                "#..#..#",
                "#.....#",
                ".#...#.",
                "..#.#..",
                "...#...",
        };
        private static final float HEART_PIXEL = 0.5F;
        private static final CubeDeformation SNUG = new CubeDeformation(0.26F);

        public final ModelLayerLocation layer;
        final ResourceLocation texture;
        final int textureW;
        final int textureH;

        Piece(int textureW, int textureH) {
            String name = name().toLowerCase(Locale.ROOT);
            this.layer = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, name), "main");
            this.texture = ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "textures/entity/" + name + ".png");
            this.textureW = textureW;
            this.textureH = textureH;
        }

        public LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            switch (this) {
                // A leather band hugging the top of the torso where the neck is, with an open metal heart ring in front
                case CHOKER -> {
                    CubeListBuilder choker = CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-4.0F, -0.5F, -2.0F, 8.0F, 1.0F, 4.0F, new CubeDeformation(0.3F));
                    // The heart outline, one half-pixel cube per filled cell, centred on the front of the band
                    for (int row = 0; row < HEART.length; row++) {
                        for (int col = 0; col < HEART[row].length(); col++) {
                            if (HEART[row].charAt(col) == '#') {
                                float x = (col - HEART[row].length() / 2.0F) * HEART_PIXEL;
                                float y = -0.7F + row * HEART_PIXEL;
                                choker.texOffs(0, 8).addBox(x, y, -2.75F, HEART_PIXEL, HEART_PIXEL, 0.4F);
                            }
                        }
                    }
                    root.addOrReplaceChild("body", choker, PartPose.ZERO);
                }
                // A top over the chest, stopping well above the waist, with little caps over the shoulders
                case CROP_TOP -> {
                    root.addOrReplaceChild("body", CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 6.0F, 4.0F, new CubeDeformation(0.27F)), PartPose.ZERO);
                    addArms(root, 16, -2.0F, 3.0F);
                }
                // A pleated skirt: waistband at the hips, flaring out over the top of the legs
                case SKIRT -> root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.5F, 9.5F, -2.5F, 9.0F, 2.0F, 5.0F, new CubeDeformation(0.05F))
                        .texOffs(0, 8).addBox(-5.0F, 11.5F, -3.0F, 10.0F, 5.0F, 6.0F, new CubeDeformation(0.05F)), PartPose.ZERO);
                // Stockings from the foot to high on the thigh
                case THIGH_HIGHS -> {
                    root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-2.0F, 2.0F, -2.0F, 4.0F, 10.0F, 4.0F, SNUG), PartPose.ZERO);
                    root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                            .texOffs(16, 0).addBox(-2.0F, 2.0F, -2.0F, 4.0F, 10.0F, 4.0F, SNUG), PartPose.ZERO);
                }
                // Sleeves from below the elbow to the knuckles
                case HAND_WARMERS -> addArms(root, 0, 3.0F, 7.0F);
                // Gauntlets over the hand and wrist
                case TACTICAL_GLOVES -> addArms(root, 0, 6.0F, 4.0F);
                // A face mask over the mouth and nose, in front of the skin's hat layer, with straps back to the ears
                case MASK -> root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.5F, -3.0F, -5.0F, 9.0F, 3.0F, 0.5F)
                        .texOffs(0, 8).addBox(-4.75F, -2.4F, -4.8F, 0.25F, 0.4F, 4.3F)
                        .texOffs(0, 8).addBox(4.5F, -2.4F, -4.8F, 0.25F, 0.4F, 4.3F), PartPose.ZERO);
                // Two pointed ears on top of the head (over the hat layer), tilted a little outwards, pink inside
                case CAT_EARS -> {
                    PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
                    for (int side = -1; side <= 1; side += 2) {
                        head.addOrReplaceChild(side < 0 ? "right_ear" : "left_ear", CubeListBuilder.create()
                                        .texOffs(0, 0).addBox(-1.5F, -1.5F, -0.5F, 3.0F, 1.5F, 1.0F)
                                        .texOffs(0, 0).addBox(-1.0F, -2.5F, -0.5F, 2.0F, 1.0F, 1.0F)
                                        .texOffs(0, 0).addBox(-0.5F, -3.5F, -0.5F, 1.0F, 1.0F, 1.0F)
                                        .texOffs(0, 8).addBox(-0.75F, -1.5F, -0.75F, 1.5F, 1.5F, 0.25F)
                                        .texOffs(0, 8).addBox(-0.5F, -2.5F, -0.75F, 1.0F, 1.0F, 0.25F),
                                PartPose.offsetAndRotation(side * 2.5F, -8.5F, -1.0F, 0.0F, 0.0F, side * 0.25F));
                    }
                }
                // A tail from the small of the back, a chain of segments curving back and up, with a white tip
                case CAT_TAIL -> {
                    PartDefinition parent = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
                    for (int i = 0; i < TAIL_BENDS.length; i++) {
                        boolean tip = i == TAIL_BENDS.length - 1;
                        parent = parent.addOrReplaceChild("tail_" + i, CubeListBuilder.create()
                                        .texOffs(0, tip ? 8 : 0).addBox(-0.75F, 0.0F, -0.75F, 1.5F, TAIL_SEGMENT, 1.5F),
                                PartPose.offsetAndRotation(0.0F, i == 0 ? 10.5F : TAIL_SEGMENT - 0.3F, i == 0 ? 2.0F : 0.0F,
                                        TAIL_BENDS[i], 0.0F, 0.0F));
                    }
                }
            }
            return LayerDefinition.create(mesh, textureW, textureH);
        }

        /**
         * A sleeve on each arm from {@code top} down {@code height} pixels, in both arm widths (classic 4 wide, slim
         * 3 wide), with their textures side by side in a row starting at {@code v}.
         */
        private static void addArms(PartDefinition root, int v, float top, float height) {
            root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                    .texOffs(0, v).addBox(-3.0F, top, -2.0F, 4.0F, height, 4.0F, SNUG), PartPose.ZERO);
            root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                    .texOffs(16, v).addBox(-1.0F, top, -2.0F, 4.0F, height, 4.0F, SNUG), PartPose.ZERO);
            root.addOrReplaceChild("right_arm_slim", CubeListBuilder.create()
                    .texOffs(32, v).addBox(-2.0F, top, -2.0F, 3.0F, height, 4.0F, SNUG), PartPose.ZERO);
            root.addOrReplaceChild("left_arm_slim", CubeListBuilder.create()
                    .texOffs(48, v).addBox(-1.0F, top, -2.0F, 3.0F, height, 4.0F, SNUG), PartPose.ZERO);
        }
    }

    private final Piece piece;
    private final ModelPart root;

    public ClothingRenderer(Piece piece) {
        this.piece = piece;
        this.root = Minecraft.getInstance().getEntityModels().bakeLayer(piece.layer);
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack poseStack,
                                                                         RenderLayerParent<T, M> parent, MultiBufferSource buffers, int light,
                                                                         float limbSwing, float limbSwingAmount, float partialTicks,
                                                                         float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(parent.getModel() instanceof HumanoidModel<?> humanoid)) {
            return;
        }
        ResourceLocation texture = piece == Piece.MASK ? MaskItem.style(stack).texture : piece.texture;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        switch (piece) {
            case THIGH_HIGHS -> {
                draw(root.getChild("right_leg"), humanoid.rightLeg, poseStack, consumer, light);
                draw(root.getChild("left_leg"), humanoid.leftLeg, poseStack, consumer, light);
            }
            case HAND_WARMERS, TACTICAL_GLOVES -> drawArms(slotContext, humanoid, poseStack, consumer, light);
            case MASK, CAT_EARS -> draw(root.getChild("head"), humanoid.head, poseStack, consumer, light);
            case CAT_TAIL -> {
                // A lazy side to side sway, faster while walking
                ModelPart base = root.getChild("body").getChild("tail_0");
                base.zRot = Mth.sin(ageInTicks * 0.08F + limbSwing * 0.5F) * (0.15F + limbSwingAmount * 0.3F);
                draw(root.getChild("body"), humanoid.body, poseStack, consumer, light);
            }
            case CROP_TOP -> {
                draw(root.getChild("body"), humanoid.body, poseStack, consumer, light);
                drawArms(slotContext, humanoid, poseStack, consumer, light);
            }
            default -> draw(root.getChild("body"), humanoid.body, poseStack, consumer, light);
        }
    }

    /** Picks the sleeves that fit the wearer's skin: slim (Alex-style) arms are a pixel narrower. */
    private void drawArms(SlotContext slotContext, HumanoidModel<?> humanoid, PoseStack poseStack, VertexConsumer consumer, int light) {
        boolean slim = slotContext.entity() instanceof AbstractClientPlayer player && "slim".equals(player.getModelName());
        String suffix = slim ? "_slim" : "";
        draw(root.getChild("right_arm" + suffix), humanoid.rightArm, poseStack, consumer, light);
        draw(root.getChild("left_arm" + suffix), humanoid.leftArm, poseStack, consumer, light);
    }

    private static void draw(ModelPart part, ModelPart follow, PoseStack poseStack, VertexConsumer consumer, int light) {
        part.copyFrom(follow);
        part.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
    }
}
