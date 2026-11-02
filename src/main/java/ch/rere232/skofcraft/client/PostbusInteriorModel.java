package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.Postbus;
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

/**
 * The cabin, in the same coordinates as {@link PostbusModel}: the nose points to -Z and +X is the bus's left. It sits
 * just inside the body, whose faces are culled from within, so the gaps left between these panels are the windows.
 * Each box takes its texture from a large swatch of one material, so any box that fits a swatch can use it.
 */
public class PostbusInteriorModel extends EntityModel<Postbus> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "postbus_interior"), "main");

    /** Model units per block at the scale the renderer draws at. */
    private static final float UNITS_PER_BLOCK = 8.0F;

    /** Where the gauges are drawn, shared with the renderer. */
    public static final float CLUSTER_LEFT = 9.8F;
    public static final float CLUSTER_RIGHT = -0.2F;
    public static final float CLUSTER_TOP = 7.4F;
    public static final float CLUSTER_BOTTOM = 11.4F;
    public static final float CLUSTER_FACE = -32.0F;
    public static final float BOARD_FACE = -24.4F;
    public static final float BOARD_TOP = 3.0F;
    public static final float BOARD_WIDTH = 12.0F;
    private static final float GAUGE_Y = 9.4F;
    private static final float SPEED_GAUGE_X = 7.3F;
    private static final float FUEL_GAUGE_X = 2.3F;
    private static final float NEEDLE_SWEEP = 2.2F;
    private static final float WHEEL_TURNS_PER_STEER = 4.0F;

    private static final int[] LINING = {0, 0};
    private static final int[] FLOOR = {200, 0};
    private static final int[] POLE = {400, 0};
    private static final int[] RED = {456, 0};
    private static final int[] DASH = {400, 40};
    private static final int[] WALL = {0, 80};
    private static final int[] FABRIC = {160, 80};
    private static final int[] HAZARD = {260, 80};
    private static final int[] METAL = {360, 80};
    private static final int[] BLACK = {436, 80};
    private static final int[] PARCEL = {260, 120};
    private static final int[] TRIM = {360, 120};
    private static final int[] LIGHT = {436, 120};

    private final ModelPart root;
    private final ModelPart wheel;
    private final ModelPart speedNeedle;
    private final ModelPart fuelNeedle;

    public PostbusInteriorModel(ModelPart root) {
        this.root = root;
        this.wheel = root.getChild("column").getChild("wheel");
        this.speedNeedle = root.getChild("speed_needle");
        this.fuelNeedle = root.getChild("fuel_needle");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        Builder b = new Builder(mesh.getRoot());
        shell(b);
        rightSide(b);
        driverStation(b);
        cabinFittings(b);
        for (int i = 0; i < Postbus.SEATS.length; i++) {
            seat(b, (float) Postbus.SEATS[i][1] * UNITS_PER_BLOCK, (float) -Postbus.SEATS[i][0] * UNITS_PER_BLOCK, i == 0);
        }
        return LayerDefinition.create(mesh, 512, 256);
    }

    private static void shell(Builder b) {
        b.box(FLOOR, -11, 19, -36, 22, 1, 72);
        b.box(LINING, -11, 1, -36, 22, 1, 72);
        // Left wall: below the windows, above them, and a pillar between each pair
        b.box(WALL, 10, 11, -36, 1, 8, 72);
        b.box(WALL, 10, 2, -36, 1, 1, 72);
        b.box(WALL, -11, 2, -36, 1, 1, 72);
        for (int z = -32; z <= 32; z += 8) {
            b.box(TRIM, 10, 3, z - 0.5F, 1, 8, 1);
        }
        // Windscreen frame
        b.box(TRIM, 9.5F, 2, -36, 1.5F, 10, 1);
        b.box(TRIM, -11, 2, -36, 1.5F, 10, 1);
        b.box(TRIM, -11, 2, -36, 22, 1.5F, 1);
        // Rear wall around the back window
        b.box(WALL, -11, 11, 35, 22, 8, 1);
        b.box(WALL, -11, 2, 35, 22, 1, 1);
        b.box(TRIM, -11, 3, 35, 2, 8, 1);
        b.box(TRIM, 9, 3, 35, 2, 8, 1);
        // Wheel arches, or the tyres would come up through the floor
        b.box(TRIM, 7.5F, 16.5F, -27.5F, 3, 2.5F, 9);
        b.box(TRIM, -10.5F, 16.5F, -27.5F, 3, 2.5F, 9);
        b.box(TRIM, 7.5F, 16.5F, 16.5F, 3, 2.5F, 11);
        b.box(TRIM, -10.5F, 16.5F, 16.5F, 3, 2.5F, 11);
    }

    /** The door side, where the panels stop so both doors are glass down to the step. */
    private static void rightSide(Builder b) {
        b.box(WALL, -11, 11, -36, 1, 8, 3);
        b.box(WALL, -11, 11, -27, 1, 8, 24);
        b.box(WALL, -11, 11, 3, 1, 8, 33);
        for (int z : new int[] {-24, -16, -8, 8, 16, 24, 32}) {
            b.box(TRIM, -11, 3, z - 0.5F, 1, 8, 1);
        }
        for (int z : new int[] {-33, -27, -3, 3}) {
            b.box(TRIM, -11, 3, z - 0.5F, 1, 16, 1);
        }
        for (int z : new int[] {-30, 0}) {
            b.box(BLACK, -10.8F, 3, z - 0.2F, 0.6F, 16, 0.4F);
            b.box(POLE, -10, 7, z - 1.5F, 0.4F, 11, 0.4F);
        }
        b.box(HAZARD, -10, 18.9F, -33, 3, 0.1F, 6);
        b.box(HAZARD, -10, 18.9F, -3, 3, 0.1F, 6);
    }

    private static void driverStation(Builder b) {
        b.box(DASH, -11, 12, -36, 22, 7, 4);
        b.box(DASH, CLUSTER_RIGHT, CLUSTER_TOP, -34, CLUSTER_LEFT - CLUSTER_RIGHT, 12 - CLUSTER_TOP, 2);
        b.box(METAL, -2, 3.5F, -35, 4, 1, 0.5F);

        PartDefinition column = b.parts.addOrReplaceChild("column", CubeListBuilder.create()
                .texOffs(BLACK[0], BLACK[1]).addBox(-0.3F, -0.3F, -2.5F, 0.6F, 0.6F, 2.5F),
                PartPose.offsetAndRotation(4.8F, 11.8F, -30.5F, 0.35F, 0.0F, 0.0F));
        column.addOrReplaceChild("wheel", CubeListBuilder.create()
                .texOffs(BLACK[0], BLACK[1])
                .addBox(-2.4F, -2.4F, 0.0F, 4.8F, 0.6F, 0.6F)
                .addBox(-2.4F, 1.8F, 0.0F, 4.8F, 0.6F, 0.6F)
                .addBox(-2.4F, -1.8F, 0.0F, 0.6F, 3.6F, 0.6F)
                .addBox(1.8F, -1.8F, 0.0F, 0.6F, 3.6F, 0.6F)
                .texOffs(METAL[0], METAL[1])
                .addBox(-1.8F, -0.2F, 0.1F, 3.6F, 0.4F, 0.4F)
                .addBox(-0.2F, 0.2F, 0.1F, 0.4F, 1.6F, 0.4F)
                .texOffs(RED[0], RED[1])
                .addBox(-0.7F, -0.7F, -0.1F, 1.4F, 1.4F, 0.8F),
                PartPose.ZERO);

        needle(b, "speed_needle", SPEED_GAUGE_X);
        needle(b, "fuel_needle", FUEL_GAUGE_X);

        // A snus tin on the dash, for the driver
        b.box(RED, -8.5F, 11.2F, -34.5F, 2.5F, 0.8F, 2.5F);
        // Ticket machine, across from the driver
        b.box(METAL, -5.5F, 12, -29, 1, 7, 1);
        b.box(POLE, -7, 7, -30, 4, 5, 2.5F);
        b.box(BLACK, -6.5F, 8, -27.6F, 3, 2, 0.1F);
    }

    private static void needle(Builder b, String name, float x) {
        b.parts.addOrReplaceChild(name, CubeListBuilder.create().texOffs(RED[0], RED[1])
                .addBox(-0.12F, -1.5F, 0.0F, 0.24F, 1.7F, 0.1F), PartPose.offset(x, GAUGE_Y, CLUSTER_FACE + 0.06F));
    }

    private static void cabinFittings(Builder b) {
        b.box(LIGHT, -0.8F, 2, -30, 1.6F, 0.1F, 60);
        // Luggage racks and hand rails
        b.box(METAL, 7, 3, -24, 3, 0.4F, 58);
        b.box(METAL, -10, 3, -24, 3, 0.4F, 58);
        b.box(POLE, 1, 3, -24, 0.4F, 0.4F, 50);
        b.box(POLE, -1.4F, 3, -24, 0.4F, 0.4F, 50);
        for (int z : new int[] {-14, 6, 14, 25}) {
            float x = z == 6 ? -1.4F : 1.0F;
            b.box(POLE, x, 2, z, 0.4F, 17, 0.4F);
            b.box(RED, x - 0.2F, 9, z - 0.2F, 0.8F, 0.8F, 0.8F);
        }
        // The board over the aisle
        b.box(BLACK, -BOARD_WIDTH / 2.0F, BOARD_TOP, BOARD_FACE - 0.5F, BOARD_WIDTH, 2, 0.5F);
        b.box(METAL, -5, 2, BOARD_FACE - 0.4F, 0.3F, 1, 0.3F);
        b.box(METAL, 4.7F, 2, BOARD_FACE - 0.4F, 0.3F, 1, 0.3F);
        // It is still the post: parcels behind the last row
        b.box(PARCEL, -9.5F, 13, 26, 6, 6, 6);
        b.box(PARCEL, -3, 14, 28, 5, 5, 5);
        b.box(PARCEL, 3, 12, 25.5F, 7, 7, 7);
        b.box(PARCEL, 4, 8, 27, 4, 4, 4);
    }

    private static void seat(Builder b, float x, float z, boolean driver) {
        int[] material = driver ? BLACK : FABRIC;
        b.box(METAL, x - 1.5F, 16, z - 1.5F, 3, 3, 3);
        b.box(material, x - 3.5F, 14, z - 3.5F, 7, 2, 7);
        b.box(material, x - 3.5F, 7, z + 3, 7, 7, 1.2F);
        b.box(BLACK, x - 2, 5.8F, z + 3.1F, 4, 1.2F, 1);
    }

    @Override
    public void setupAnim(Postbus bus, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float partial = ageInTicks - bus.tickCount;
        // Turning left is a negative steer, and a positive roll here is anticlockwise from the driver's seat
        wheel.zRot = -Mth.lerp(partial, bus.steerO, bus.steer) * WHEEL_TURNS_PER_STEER;
        speedNeedle.zRot = needleAngle(Mth.lerp(partial, bus.shownSpeedO, bus.shownSpeed));
        fuelNeedle.zRot = needleAngle(bus.fuelFraction());
    }

    private static float needleAngle(float fraction) {
        return NEEDLE_SWEEP - 2.0F * NEEDLE_SWEEP * Mth.clamp(fraction, 0.0F, 1.0F);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
                               float red, float green, float blue, float alpha) {
        root.render(poseStack, buffer, light, overlay, red, green, blue, alpha);
    }

    private static final class Builder {
        final PartDefinition parts;
        private int id;

        Builder(PartDefinition parts) {
            this.parts = parts;
        }

        void box(int[] swatch, float x, float y, float z, float w, float h, float d) {
            parts.addOrReplaceChild("box_" + id++, CubeListBuilder.create().texOffs(swatch[0], swatch[1]).addBox(x, y, z, w, h, d), PartPose.ZERO);
        }
    }
}
