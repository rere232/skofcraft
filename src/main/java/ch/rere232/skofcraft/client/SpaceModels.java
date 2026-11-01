package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;

/** The launcher rocket and the strike satellite's tungsten rod. Both are built y-up with the origin at the bottom. */
public final class SpaceModels {
    public static final ModelLayerLocation ROCKET =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "launch_rocket"), "main");
    public static final ModelLayerLocation ROD =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "orbital_rod"), "main");
    public static final ResourceLocation ROCKET_TEXTURE = ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "textures/entity/launch_rocket.png");
    public static final ResourceLocation ICBM_TEXTURE = ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "textures/entity/giga_icbm.png");
    /** Height of the rocket model in blocks, from the bottom of the engine bell to the nose tip. */
    public static final double ROCKET_HEIGHT = 86.0D / 16.0D;
    public static final ResourceLocation ROD_TEXTURE = ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "textures/entity/orbital_rod.png");

    private SpaceModels() {}

    /** About five and a half blocks tall: engine bell, first stage with four fins, second stage, fairing. */
    public static LayerDefinition createRocketLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("rocket", CubeListBuilder.create()
                .texOffs(88, 0).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 4.0F, 8.0F)
                .texOffs(0, 0).addBox(-6.0F, 4.0F, -6.0F, 12.0F, 56.0F, 12.0F)
                .texOffs(48, 0).addBox(-5.0F, 60.0F, -5.0F, 10.0F, 16.0F, 10.0F)
                .texOffs(48, 26).addBox(-3.5F, 76.0F, -3.5F, 7.0F, 6.0F, 7.0F)
                .texOffs(48, 39).addBox(-1.5F, 82.0F, -1.5F, 3.0F, 4.0F, 3.0F)
                .texOffs(88, 12).addBox(6.0F, 4.0F, -0.5F, 5.0F, 14.0F, 1.0F)
                .texOffs(88, 12).addBox(-11.0F, 4.0F, -0.5F, 5.0F, 14.0F, 1.0F)
                .texOffs(88, 28).addBox(-0.5F, 4.0F, 6.0F, 1.0F, 14.0F, 5.0F)
                .texOffs(88, 28).addBox(-0.5F, 4.0F, -11.0F, 1.0F, 14.0F, 5.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition createRodLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("rod", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 48.0F, 3.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
