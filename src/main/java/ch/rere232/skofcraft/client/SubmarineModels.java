package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.NuclearSubmarine;
import ch.rere232.skofcraft.entity.Torpedo;
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

/** The submarine and its torpedo. */
public final class SubmarineModels {
    private SubmarineModels() {}

    /**
     * An Ohio-class boat: 170 by 13 metres in real life, so 48 by 4 blocks here. The renderer draws 8 model units to the
     * block, the bow points to -Z and the keel sits on y = 24. The hull is a stack of slices, each one a stepped circle.
     */
    public static class Boat extends EntityModel<NuclearSubmarine> {
        public static final ModelLayerLocation LAYER =
                new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "nuclear_submarine"), "main");

        private static final int[] BLACK = {0, 0};
        private static final int[] RED = {256, 0};
        private static final int[] DECK = {0, 128};
        private static final int[] SAIL = {0, 192};
        private static final int[] BRONZE = {256, 128};
        private static final int[] HATCH = {384, 128};

        private static final int HALF_LENGTH = 192;
        private static final float RADIUS = 16.0F;
        private static final float CENTER_Y = 8.0F;
        private static final int SLICE = 16;
        private static final int BANDS = 7;
        /** Below this the hull is painted red, as on the real thing. */
        private static final float BOOT_TOP = 4.0F;
        private static final int BOW_START = -130;
        private static final int STERN_START = 110;
        private static final float STERN_RADIUS = 3.0F;
        private static final int ROWS = 8;
        private static final float HATCH_OPEN = 1.7F;
        private static final int PROP_BLADES = 7;

        private final ModelPart root;
        private final ModelPart prop;
        private final ModelPart[] hatches = new ModelPart[NuclearSubmarine.MISSILE_TUBES];

        public Boat(ModelPart root) {
            this.root = root;
            this.prop = root.getChild("prop");
            for (int i = 0; i < hatches.length; i++) {
                hatches[i] = root.getChild("hatch_" + i);
            }
        }

        private static float radiusAt(float z) {
            if (z < BOW_START) {
                float t = (BOW_START - z) / (float) (BOW_START + HALF_LENGTH);
                return Math.max(STERN_RADIUS, RADIUS * Mth.sqrt(Math.max(0.0F, 1.0F - t * t)));
            }
            if (z > STERN_START) {
                float t = (z - STERN_START) / (float) (HALF_LENGTH - STERN_START);
                return Mth.lerp(t, RADIUS, STERN_RADIUS);
            }
            return RADIUS;
        }

        private static CubeListBuilder box(int[] swatch, float x, float y, float z, float w, float h, float d) {
            return CubeListBuilder.create().texOffs(swatch[0], swatch[1]).addBox(x, y, z, w, h, d);
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition parts = mesh.getRoot();

            for (int z = -HALF_LENGTH; z < HALF_LENGTH; z += SLICE) {
                float radius = radiusAt(z + SLICE / 2.0F);
                float bandHeight = radius * 2.0F / BANDS;
                for (int band = 0; band < BANDS; band++) {
                    float top = CENTER_Y - radius + band * bandHeight;
                    float middle = top + bandHeight / 2.0F - CENTER_Y;
                    float half = Mth.sqrt(Math.max(0.5F, radius * radius - middle * middle));
                    parts.addOrReplaceChild("hull_" + z + "_" + band,
                            box(top + bandHeight / 2.0F > BOOT_TOP ? RED : BLACK, -half, top, z, half * 2.0F, bandHeight, SLICE), PartPose.ZERO);
                }
            }

            // The flat walkway on top, and the raised deck over the missile tubes behind the sail
            parts.addOrReplaceChild("walkway", box(DECK, -5, -8.5F, -128, 10, 1, 64), PartPose.ZERO);
            parts.addOrReplaceChild("walkway_aft", box(DECK, -5, -8.5F, 40, 10, 1, 64), PartPose.ZERO);
            parts.addOrReplaceChild("missile_deck", box(DECK, -11, -11, -84, 22, 3, 64), PartPose.ZERO);
            parts.addOrReplaceChild("missile_deck_aft", box(DECK, -11, -11, -20, 22, 3, 60), PartPose.ZERO);

            // The sail, with its dive planes and masts
            parts.addOrReplaceChild("sail", box(SAIL, -6, -36, -116, 12, 28, 32), PartPose.ZERO);
            parts.addOrReplaceChild("sail_planes", box(BLACK, -20, -24, -112, 40, 2, 10), PartPose.ZERO);
            parts.addOrReplaceChild("periscope", box(HATCH, -1, -46, -108, 2, 10, 2), PartPose.ZERO);
            parts.addOrReplaceChild("mast", box(HATCH, -1, -42, -100, 2, 6, 2), PartPose.ZERO);

            // Rudder and stern planes, right at the tail
            parts.addOrReplaceChild("rudder", box(BLACK, -1, -10, 160, 2, 36, 18), PartPose.ZERO);
            parts.addOrReplaceChild("stern_planes", box(BLACK, -22, 7, 164, 44, 2, 14), PartPose.ZERO);

            // The propeller, on its own part so it can turn
            PartDefinition prop = parts.addOrReplaceChild("prop", box(BRONZE, -2, -2, 0, 4, 4, 6), PartPose.offset(0, CENTER_Y, HALF_LENGTH - 2));
            for (int i = 0; i < PROP_BLADES; i++) {
                prop.addOrReplaceChild("blade_" + i, box(BRONZE, -2, -15, 1, 4, 14, 1),
                        PartPose.rotation(0.0F, 0.0F, Mth.TWO_PI * i / PROP_BLADES));
            }

            // Eight pairs of hatches, hinged on their outer edges
            for (int i = 0; i < NuclearSubmarine.MISSILE_TUBES; i++) {
                int row = i / 2;
                boolean port = i % 2 == 0;
                float z = -74 + (float) row * 112 / ROWS;
                parts.addOrReplaceChild("hatch_" + i, box(HATCH, port ? -9 : 0, -1, -5, 9, 1, 10),
                        PartPose.offset(port ? 10 : -10, -11, z));
            }
            return LayerDefinition.create(mesh, 512, 256);
        }

        @Override
        public void setupAnim(NuclearSubmarine boat, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            float partial = ageInTicks - boat.tickCount;
            prop.zRot = Mth.lerp(partial, boat.propAngleO, boat.propAngle);
            float open = Mth.lerp(partial, boat.hatchAngleO, boat.hatchAngle) * HATCH_OPEN;
            int hatch = boat.getOpenHatch();
            for (int i = 0; i < hatches.length; i++) {
                hatches[i].zRot = i == hatch ? (i % 2 == 0 ? open : -open) : 0.0F;
            }
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, float red, float green, float blue, float alpha) {
            root.render(poseStack, buffer, light, overlay, red, green, blue, alpha);
        }
    }

    /** About 6 metres long, which comes out a bit fat at this scale. The nose points to +Z. */
    public static class Fish extends EntityModel<Torpedo> {
        public static final ModelLayerLocation LAYER =
                new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "torpedo"), "main");

        private final ModelPart root;

        public Fish(ModelPart root) {
            this.root = root;
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition parts = mesh.getRoot();
            parts.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -3, -18, 6, 6, 32), PartPose.ZERO);
            parts.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(0, 38).addBox(-2, -2, 14, 4, 4, 4), PartPose.ZERO);
            parts.addOrReplaceChild("fins", CubeListBuilder.create().texOffs(16, 38).addBox(-5, -0.5F, -18, 10, 1, 4)
                    .texOffs(16, 44).addBox(-0.5F, -5, -18, 1, 10, 4), PartPose.ZERO);
            return LayerDefinition.create(mesh, 128, 64);
        }

        @Override
        public void setupAnim(Torpedo torpedo, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, float red, float green, float blue, float alpha) {
            root.render(poseStack, buffer, light, overlay, red, green, blue, alpha);
        }
    }
}
