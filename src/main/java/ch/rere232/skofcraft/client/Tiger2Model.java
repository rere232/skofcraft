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

/** Tiger II with the flat-faced Henschel turret, from its real proportions: about 7.4 by 3.75 by 3.1 metres. */
public class Tiger2Model extends TankModel {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "tiger_2"), "main");

    private static final int[] ARMOR = {0, 0};
    private static final int[] ARMOR_DARK = {256, 0};
    private static final int[] TRACK = {0, 128};
    private static final int[] BARREL = {256, 128};
    private static final int[] WHEEL = {256, 192};
    private static final int[] HATCH = {384, 128};
    private static final int ROAD_WHEELS = 9;

    public Tiger2Model(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();

        // Wide tracks, and the big overlapping road wheels the real one had
        parts.addOrReplaceChild("track_left", box(TRACK, 9, 15, -29, 6, 9, 59), PartPose.ZERO);
        parts.addOrReplaceChild("track_right", box(TRACK, -15, 15, -29, 6, 9, 59), PartPose.ZERO);
        for (int i = 0; i < ROAD_WHEELS; i++) {
            float z = -25 + i * 5.5F;
            float inset = i % 2 == 0 ? 0.0F : 0.3F;
            parts.addOrReplaceChild("wheel_left_" + i, box(WHEEL, 15 - inset, 17, z, 0.5F, 6, 6), PartPose.ZERO);
            parts.addOrReplaceChild("wheel_right_" + i, box(WHEEL, -15.5F + inset, 17, z, 0.5F, 6, 6), PartPose.ZERO);
        }

        // Hull: the slab sides overhang the tracks
        parts.addOrReplaceChild("hull_lower", box(ARMOR_DARK, -9, 12, -27, 18, 9, 56), PartPose.ZERO);
        parts.addOrReplaceChild("hull_upper", box(ARMOR, -14.5F, 7, -22, 29, 8, 51), PartPose.ZERO);
        // The long sloped glacis, stepped since these are boxes
        parts.addOrReplaceChild("glacis_1", box(ARMOR, -14, 8, -24, 28, 3, 2), PartPose.ZERO);
        parts.addOrReplaceChild("glacis_2", box(ARMOR, -14, 11, -26, 28, 3, 2), PartPose.ZERO);
        parts.addOrReplaceChild("glacis_3", box(ARMOR, -14, 14, -28, 28, 3, 2), PartPose.ZERO);
        parts.addOrReplaceChild("glacis_4", box(ARMOR_DARK, -12, 17, -29, 24, 2, 1), PartPose.ZERO);

        // Engine deck, its grilles and the two exhausts up the rear plate
        parts.addOrReplaceChild("deck", box(ARMOR, -12, 6, 8, 24, 1, 20), PartPose.ZERO);
        parts.addOrReplaceChild("grille_left", box(HATCH, 2, 5.5F, 11, 8, 1, 14), PartPose.ZERO);
        parts.addOrReplaceChild("grille_right", box(HATCH, -10, 5.5F, 11, 8, 1, 14), PartPose.ZERO);
        parts.addOrReplaceChild("exhaust_left", box(BARREL, 4, 7, 29, 2, 7, 2), PartPose.ZERO);
        parts.addOrReplaceChild("exhaust_right", box(BARREL, -6, 7, 29, 2, 7, 2), PartPose.ZERO);

        // Driver and radio hatches
        parts.addOrReplaceChild("driver_hatch", box(HATCH, 3, 6.5F, -20, 5, 0.5F, 5), PartPose.ZERO);
        parts.addOrReplaceChild("radio_hatch", box(HATCH, -8, 6.5F, -20, 5, 0.5F, 5), PartPose.ZERO);

        // The flat-faced turret, with its narrow front and the roof set in from the sides
        PartDefinition turret = parts.addOrReplaceChild("turret", CubeListBuilder.create()
                .texOffs(ARMOR[0], ARMOR[1]).addBox(-11, -7, -10, 22, 7, 24)
                .addBox(-8, -7, -13, 16, 7, 3)
                .texOffs(ARMOR_DARK[0], ARMOR_DARK[1]).addBox(-9, -8, -8, 18, 1, 20),
                PartPose.offset(0, 7, -2));
        turret.addOrReplaceChild("cupola", box(HATCH, 4, -10, 2, 5, 2, 5), PartPose.ZERO);
        turret.addOrReplaceChild("loader_hatch", box(HATCH, -8, -8.5F, 3, 5, 0.5F, 5), PartPose.ZERO);
        // Spare track links hung on the turret sides
        turret.addOrReplaceChild("spare_links_left", box(TRACK, 11, -5, -6, 0.5F, 3, 14), PartPose.ZERO);
        turret.addOrReplaceChild("spare_links_right", box(TRACK, -11.5F, -5, -6, 0.5F, 3, 14), PartPose.ZERO);

        // The long 88 mm gun in its rounded "pig's head" mantlet, ending in a double muzzle brake
        turret.addOrReplaceChild("gun", CubeListBuilder.create()
                .texOffs(ARMOR_DARK[0], ARMOR_DARK[1]).addBox(-3, -3, -3, 6, 6, 4)
                .addBox(-2, -2, -5, 4, 4, 2)
                .texOffs(BARREL[0], BARREL[1]).addBox(-0.75F, -0.75F, -36, 1.5F, 1.5F, 31)
                .addBox(-1.5F, -1, -38, 3, 2, 1)
                .addBox(-1.5F, -1, -36, 3, 2, 1)
                .addBox(-0.5F, -0.5F, -38, 1, 1, 2)
                .addBox(3.5F, -0.5F, -7, 1, 1, 5),
                PartPose.offset(0, -4, -12));
        return LayerDefinition.create(mesh, 512, 256);
    }
}
