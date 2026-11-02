package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/** Leopard 1, from its real proportions: about 7.1 by 3.4 by 2.8 metres, low and with a rounded cast turret. */
public class Leopard1Model extends TankModel {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "leopard_1"), "main");

    private static final int[] ARMOR = {0, 0};
    private static final int[] ARMOR_DARK = {256, 0};
    private static final int[] TRACK = {0, 128};
    private static final int[] BARREL = {256, 128};
    private static final int[] WHEEL = {256, 192};
    private static final int[] HATCH = {384, 128};
    private static final int ROAD_WHEELS = 7;

    public Leopard1Model(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();

        // Tracks, with the road wheels showing through and thin skirts along the top run
        parts.addOrReplaceChild("track_left", box(TRACK, 9, 16, -26, 4.5F, 8, 53), PartPose.ZERO);
        parts.addOrReplaceChild("track_right", box(TRACK, -13.5F, 16, -26, 4.5F, 8, 53), PartPose.ZERO);
        for (int i = 0; i < ROAD_WHEELS; i++) {
            float z = -22 + i * 7;
            parts.addOrReplaceChild("wheel_left_" + i, box(WHEEL, 13.5F, 18, z, 0.5F, 5, 5), PartPose.ZERO);
            parts.addOrReplaceChild("wheel_right_" + i, box(WHEEL, -14, 18, z, 0.5F, 5, 5), PartPose.ZERO);
        }
        parts.addOrReplaceChild("skirt_left", box(ARMOR_DARK, 14, 14.5F, -24, 0.5F, 3.5F, 50), PartPose.ZERO);
        parts.addOrReplaceChild("skirt_right", box(ARMOR_DARK, -14.5F, 14.5F, -24, 0.5F, 3.5F, 50), PartPose.ZERO);

        // Hull, narrow below the track guards and wide above them
        parts.addOrReplaceChild("hull_lower", box(ARMOR_DARK, -9, 12, -26, 18, 9, 54), PartPose.ZERO);
        parts.addOrReplaceChild("hull_upper", box(ARMOR, -14, 11, -23, 28, 4, 51), PartPose.ZERO);
        // The sloped nose, stepped since these are boxes
        parts.addOrReplaceChild("glacis_high", box(ARMOR, -13, 12, -25, 26, 3, 2), PartPose.ZERO);
        parts.addOrReplaceChild("glacis_mid", box(ARMOR, -13, 15, -27, 26, 3, 2), PartPose.ZERO);
        parts.addOrReplaceChild("glacis_low", box(ARMOR_DARK, -11, 18, -28, 22, 3, 2), PartPose.ZERO);

        // Engine deck, its grilles and the rear plate with the exhausts on it
        parts.addOrReplaceChild("deck", box(ARMOR, -11, 10, 9, 22, 1, 19), PartPose.ZERO);
        parts.addOrReplaceChild("grille_left", box(HATCH, 2, 9.5F, 12, 7, 1, 13), PartPose.ZERO);
        parts.addOrReplaceChild("grille_right", box(HATCH, -9, 9.5F, 12, 7, 1, 13), PartPose.ZERO);
        parts.addOrReplaceChild("exhaust_left", box(BARREL, 11, 12, 28, 2, 2, 1), PartPose.ZERO);
        parts.addOrReplaceChild("exhaust_right", box(BARREL, -13, 12, 28, 2, 2, 1), PartPose.ZERO);

        // Driver's hatch, headlights and the stowage boxes on the track guards
        parts.addOrReplaceChild("driver_hatch", box(HATCH, -8, 10.5F, -21, 5, 0.5F, 5), PartPose.ZERO);
        parts.addOrReplaceChild("light_left", box(HATCH, 9, 11, -25, 2, 2, 2), PartPose.ZERO);
        parts.addOrReplaceChild("light_right", box(HATCH, -11, 11, -25, 2, 2, 2), PartPose.ZERO);
        parts.addOrReplaceChild("stowage_left", box(ARMOR_DARK, 11, 9.5F, 12, 3, 1.5F, 12), PartPose.ZERO);
        parts.addOrReplaceChild("stowage_right", box(ARMOR_DARK, -14, 9.5F, 12, 3, 1.5F, 12), PartPose.ZERO);

        // The cast turret, rounded off in steps, with its bustle and a nose to mount the gun in
        PartDefinition turret = parts.addOrReplaceChild("turret", CubeListBuilder.create()
                .texOffs(ARMOR[0], ARMOR[1]).addBox(-10, -4, -9, 20, 4, 20)
                .addBox(-9, -7, -8, 18, 3, 18)
                .addBox(-7, -9, -6, 14, 2, 14)
                .addBox(-6, -7, -11, 12, 6, 2)
                .addBox(-8, -6, 11, 16, 5, 4),
                PartPose.offset(0, 11, -5));
        turret.addOrReplaceChild("cupola", box(HATCH, 2, -11, -1, 5, 2, 5), PartPose.ZERO);
        turret.addOrReplaceChild("loader_hatch", box(HATCH, -7, -9.5F, 0, 5, 0.5F, 5), PartPose.ZERO);
        turret.addOrReplaceChild("smoke_left", box(BARREL, 10, -6, -6, 1, 2, 5), PartPose.ZERO);
        turret.addOrReplaceChild("smoke_right", box(BARREL, -11, -6, -6, 1, 2, 5), PartPose.ZERO);
        turret.addOrReplaceChild("basket", box(ARMOR_DARK, -7, -5, 15, 14, 3, 2), PartPose.ZERO);

        // The 105 mm gun, with the bulge of its fume extractor about halfway along
        turret.addOrReplaceChild("gun", CubeListBuilder.create()
                .texOffs(ARMOR_DARK[0], ARMOR_DARK[1]).addBox(-4, -3, -3, 8, 6, 3)
                .texOffs(BARREL[0], BARREL[1]).addBox(-0.75F, -0.75F, -34, 1.5F, 1.5F, 32)
                .addBox(-1.25F, -1.25F, -22, 2.5F, 2.5F, 7)
                .addBox(-1, -1, -34, 2, 2, 2)
                .addBox(2.5F, -0.5F, -8, 1, 1, 6),
                PartPose.offset(0, -4, -9));
        return LayerDefinition.create(mesh, 512, 256);
    }
}
