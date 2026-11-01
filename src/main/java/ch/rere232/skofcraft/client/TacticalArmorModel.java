package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The tactical armor's 3D model: the vanilla armor shapes (top half of each 64x64 texture uses the usual armor
 * layout) with extra gear on top. Outer layer (helmet, plate carrier, boots): a gas mask with a filter, a visor over
 * the eyes, ear defenders, a night vision mount, front and back plates with pouches, and shoulder pads. Inner layer
 * (leggings): knee pads, a thigh holster and belt pouches.
 */
public final class TacticalArmorModel {
    public static final ModelLayerLocation OUTER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "tactical_armor"), "outer");
    public static final ModelLayerLocation INNER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "tactical_armor"), "inner");

    @Nullable
    private static HumanoidModel<LivingEntity> outer;
    @Nullable
    private static HumanoidModel<LivingEntity> inner;

    private TacticalArmorModel() {}

    public static HumanoidModel<LivingEntity> get(EquipmentSlot slot) {
        if (slot == EquipmentSlot.LEGS) {
            if (inner == null) {
                inner = new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(INNER));
            }
            return inner;
        }
        if (outer == null) {
            outer = new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(OUTER));
        }
        return outer;
    }

    public static LayerDefinition createOuterLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(1.0F), 0.0F);
        PartDefinition root = mesh.getRoot();
        // Head: the armored head box spans x -5..5, y -9..1, z -5..5
        root.getChild("head").addOrReplaceChild("gear", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-2.5F, -4.0F, -7.0F, 5.0F, 4.0F, 2.0F)      // gas mask snout
                .texOffs(14, 32).addBox(-1.5F, -3.5F, -9.0F, 3.0F, 3.0F, 2.0F)     // filter canister
                .texOffs(24, 32).addBox(-4.0F, -5.5F, -6.0F, 8.0F, 2.0F, 1.0F)     // visor
                .texOffs(42, 32).addBox(-1.0F, -8.5F, -6.5F, 2.0F, 2.0F, 2.0F)     // night vision mount
                .texOffs(50, 32).addBox(-6.0F, -5.0F, -1.5F, 1.0F, 3.0F, 3.0F)     // ear defenders
                .texOffs(50, 32).mirror().addBox(5.0F, -5.0F, -1.5F, 1.0F, 3.0F, 3.0F), PartPose.ZERO);
        // Body: x -5..5, y -1..13, z -3..3
        root.getChild("body").addOrReplaceChild("gear", CubeListBuilder.create()
                .texOffs(0, 40).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 8.0F, 1.0F)       // front plate
                .texOffs(18, 40).addBox(-4.0F, 0.0F, 3.0F, 8.0F, 8.0F, 1.0F)       // back plate
                .texOffs(36, 40).addBox(-4.5F, 8.5F, -4.6F, 3.0F, 3.0F, 1.0F)      // magazine pouches
                .texOffs(36, 40).addBox(-1.5F, 8.5F, -4.6F, 3.0F, 3.0F, 1.0F)
                .texOffs(36, 40).addBox(1.5F, 8.5F, -4.6F, 3.0F, 3.0F, 1.0F), PartPose.ZERO);
        // Arms: shoulder pads over the top and outside of each shoulder
        root.getChild("right_arm").addOrReplaceChild("gear", CubeListBuilder.create()
                .texOffs(0, 50).addBox(-5.0F, -4.0F, -3.5F, 4.0F, 3.0F, 7.0F), PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("gear", CubeListBuilder.create()
                .texOffs(0, 50).mirror().addBox(1.0F, -4.0F, -3.5F, 4.0F, 3.0F, 7.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createInnerLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.5F), 0.0F);
        PartDefinition root = mesh.getRoot();
        // Legs: x -2.5..2.5, y -0.5..12.5, z -2.5..2.5
        root.getChild("right_leg").addOrReplaceChild("gear", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-3.5F, 1.0F, -1.5F, 1.0F, 5.0F, 3.0F)       // thigh holster
                .texOffs(16, 32).addBox(-2.0F, 4.0F, -3.5F, 4.0F, 3.0F, 1.0F), PartPose.ZERO);  // knee pad
        root.getChild("left_leg").addOrReplaceChild("gear", CubeListBuilder.create()
                .texOffs(16, 32).mirror().addBox(-2.0F, 4.0F, -3.5F, 4.0F, 3.0F, 1.0F), PartPose.ZERO);
        // Belt pouches on the front of the hips
        root.getChild("body").addOrReplaceChild("gear", CubeListBuilder.create()
                .texOffs(28, 32).addBox(-3.5F, 9.0F, -3.5F, 3.0F, 2.0F, 1.0F)
                .texOffs(28, 32).addBox(0.5F, 9.0F, -3.5F, 3.0F, 2.0F, 1.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
