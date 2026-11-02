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

/** Panzer VIII Maus, from its real proportions: about 10 by 3.7 by 3.6 metres. */
public class MausModel extends TankModel {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "maus"), "main");

    private static final int[] ARMOR = {0, 0};
    private static final int[] ARMOR_DARK = {200, 0};
    private static final int[] TRACK = {0, 100};
    private static final int[] BARREL = {200, 100};
    private static final int[] HATCH = {300, 100};

    public MausModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();

        // Tracks and the side skirts over them, with the hull between
        parts.addOrReplaceChild("track_left", box(TRACK, 8, 12, -40, 7, 12, 80), PartPose.ZERO);
        parts.addOrReplaceChild("track_right", box(TRACK, -15, 12, -40, 7, 12, 80), PartPose.ZERO);
        parts.addOrReplaceChild("skirt_left", box(ARMOR_DARK, 13.5F, 8, -38, 1.5F, 10, 76), PartPose.ZERO);
        parts.addOrReplaceChild("skirt_right", box(ARMOR_DARK, -15, 8, -38, 1.5F, 10, 76), PartPose.ZERO);
        parts.addOrReplaceChild("hull", box(ARMOR, -13.5F, 6, -36, 27, 14, 74), PartPose.ZERO);
        // The sloped glacis, stepped since these are boxes
        parts.addOrReplaceChild("glacis_low", box(ARMOR, -13.5F, 10, -39, 27, 10, 3), PartPose.ZERO);
        parts.addOrReplaceChild("glacis_high", box(ARMOR, -13.5F, 8, -37.5F, 27, 2, 2), PartPose.ZERO);
        // Engine deck behind the turret, its grilles, and the two big exhausts
        parts.addOrReplaceChild("deck", box(ARMOR, -11, 3, 10, 22, 3, 26), PartPose.ZERO);
        parts.addOrReplaceChild("grille_left", box(HATCH, 2, 2.5F, 14, 7, 1, 16), PartPose.ZERO);
        parts.addOrReplaceChild("grille_right", box(HATCH, -9, 2.5F, 14, 7, 1, 16), PartPose.ZERO);
        parts.addOrReplaceChild("exhaust_left", box(ARMOR_DARK, 6, 0, 32, 3, 6, 3), PartPose.ZERO);
        parts.addOrReplaceChild("exhaust_right", box(ARMOR_DARK, -9, 0, 32, 3, 6, 3), PartPose.ZERO);
        parts.addOrReplaceChild("driver_hatch", box(HATCH, 3, 5, -30, 6, 1, 6), PartPose.ZERO);
        parts.addOrReplaceChild("radio_hatch", box(HATCH, -9, 5, -30, 6, 1, 6), PartPose.ZERO);

        // The turret: about as big as a small house, because it was
        PartDefinition turret = parts.addOrReplaceChild("turret", CubeListBuilder.create()
                .texOffs(ARMOR[0], ARMOR[1]).addBox(-10, -12, -11, 20, 12, 24)
                .texOffs(ARMOR_DARK[0], ARMOR_DARK[1]).addBox(-8, -13, -9, 16, 1, 20)
                .texOffs(ARMOR[0], ARMOR[1]).addBox(-8.5F, -11, -13, 17, 10, 2),
                PartPose.offset(0, 6, -4));
        turret.addOrReplaceChild("cupola", box(HATCH, 2, -15, 0, 6, 2, 6), PartPose.ZERO);
        turret.addOrReplaceChild("hatch", box(HATCH, -7, -14, 2, 5, 1, 5), PartPose.ZERO);

        turret.addOrReplaceChild("gun", CubeListBuilder.create()
                .texOffs(ARMOR_DARK[0], ARMOR_DARK[1]).addBox(-3, -3, -2, 6, 6, 3)
                .texOffs(BARREL[0], BARREL[1]).addBox(-1, -1, -44, 2, 2, 42)
                .addBox(-1.5F, -1.5F, -24, 3, 3, 8)
                .addBox(-1.5F, -1.5F, -47, 3, 3, 4)
                // The smaller gun beside the big one, which the real turret had too
                .addBox(3.5F, -0.5F, -14, 1, 1, 12),
                PartPose.offset(0, -6, -13));
        return LayerDefinition.create(mesh, 512, 256);
    }
}
