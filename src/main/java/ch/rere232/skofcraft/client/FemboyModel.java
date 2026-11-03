package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.Femboy;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Player height on a slimmer frame, with real hair, ears, a tail and a pleated skirt that swings with the legs. All the
 * looks share one mesh and one texture layout, and each variant shows its own hairstyle, ears and tail.
 */
public class FemboyModel extends HumanoidModel<Femboy> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "femboy"), "main");

    private static final int TEXTURE_SIZE = 128;
    private static final int PANELS = 8;
    private static final float PANEL_FLARE = 0.42F;
    private static final float PANEL_SWING = 0.55F;
    private static final float TWINTAIL_SPLAY = 0.22F;
    private static final float PONY_SPLAY = 0.3F;
    private static final float CAT_EAR_TILT = 0.2F;
    private static final float FOX_EAR_TILT = 0.3F;
    private static final float BUNNY_EAR_TILT = 0.1F;
    private static final int BLINK_PERIOD = 90;
    private static final int BLINK_TICKS = 3;
    private static final float WALK_FREQUENCY = 0.6662F;

    /** Which parts make up each variant, in the order of the skins. */
    private final List<Look> looks;
    private final List<ModelPart> toggled;
    private final ModelPart[] panels = new ModelPart[PANELS];
    private final ModelPart skirt;
    private final ModelPart blink;
    private final ModelPart happy;
    private final ModelPart rightTwintail;
    private final ModelPart leftTwintail;
    private final ModelPart longHair;
    private final ModelPart pony;
    private final ModelPart[] ears;
    private final float[] earTilts;

    public FemboyModel(ModelPart root) {
        super(root);
        ModelPart twintails = head.getChild("hair_twintails");
        ModelPart bob = head.getChild("hair_bob");
        ModelPart bow = head.getChild("hair_bow");
        ModelPart catEars = head.getChild("ears_cat");
        ModelPart foxEars = head.getChild("ears_fox");
        ModelPart bunnyEars = head.getChild("ears_bunny");
        ModelPart catTail = body.getChild("tail_cat");
        ModelPart foxTail = body.getChild("tail_fox");
        ModelPart bunnyTail = body.getChild("tail_bunny");
        rightTwintail = twintails.getChild("right_twintail");
        leftTwintail = twintails.getChild("left_twintail");
        longHair = head.getChild("hair_long");
        pony = head.getChild("hair_pony");
        skirt = body.getChild("skirt");
        for (int i = 0; i < PANELS; i++) {
            panels[i] = skirt.getChild("panel_" + i);
        }
        blink = head.getChild("blink");
        happy = head.getChild("happy");
        ears = new ModelPart[] {
                catEars.getChild("right_cat_ear"), catEars.getChild("left_cat_ear"),
                foxEars.getChild("right_fox_ear"), foxEars.getChild("left_fox_ear"),
                bunnyEars.getChild("right_bunny_ear"), bunnyEars.getChild("left_bunny_ear")};
        earTilts = new float[] {-CAT_EAR_TILT, CAT_EAR_TILT, -FOX_EAR_TILT, FOX_EAR_TILT, -BUNNY_EAR_TILT, BUNNY_EAR_TILT};
        ModelPart catRoot = catTail.getChild("tail_cat_0");
        ModelPart foxRoot = foxTail.getChild("tail_fox_0");
        ModelPart bunnyRoot = bunnyTail.getChild("tail_bunny_0");
        looks = List.of(
                new Look(List.of(twintails, catEars, catTail), catRoot),
                new Look(List.of(longHair, bow, foxEars, foxTail), foxRoot),
                new Look(List.of(bob, catEars, catTail), catRoot),
                new Look(List.of(pony, bunnyEars, bunnyTail), bunnyRoot));
        toggled = List.of(twintails, longHair, bob, pony, bow, catEars, foxEars, bunnyEars, catTail, foxTail, bunnyTail);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(72, 16).addBox(-3.5F, 0.0F, -1.5F, 7.0F, 7.0F, 3.0F)
                .texOffs(58, 38).addBox(-3.0F, 7.0F, -1.5F, 6.0F, 3.0F, 3.0F), PartPose.ZERO);
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-1.5F, -2.0F, -1.5F, 3.0F, 12.0F, 3.0F), PartPose.offsetAndRotation(-5.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(76, 0).addBox(-1.5F, -2.0F, -1.5F, 3.0F, 12.0F, 3.0F), PartPose.offsetAndRotation(5.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(88, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F), PartPose.offsetAndRotation(-1.7F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(100, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F), PartPose.offsetAndRotation(1.7F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        rightLeg.addOrReplaceChild("right_shoe", CubeListBuilder.create()
                .texOffs(76, 38).addBox(-1.5F, 10.0F, -2.5F, 3.0F, 2.0F, 4.0F, new CubeDeformation(0.15F)), PartPose.ZERO);
        leftLeg.addOrReplaceChild("left_shoe", CubeListBuilder.create()
                .texOffs(90, 38).addBox(-1.5F, 10.0F, -2.5F, 3.0F, 2.0F, 4.0F, new CubeDeformation(0.15F)), PartPose.ZERO);
        rightArm.addOrReplaceChild("right_cuff", CubeListBuilder.create()
                .texOffs(104, 38).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 2.0F, 4.0F), PartPose.ZERO);
        leftArm.addOrReplaceChild("left_cuff", CubeListBuilder.create()
                .texOffs(0, 46).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 2.0F, 4.0F), PartPose.ZERO);
        body.addOrReplaceChild("neck_bow", CubeListBuilder.create()
                .texOffs(0, 58).addBox(-1.5F, 1.0F, -2.0F, 3.0F, 2.0F, 1.0F), PartPose.ZERO);
        PartDefinition skirt = body.addOrReplaceChild("skirt", CubeListBuilder.create()
                .texOffs(16, 46).addBox(-3.5F, 9.0F, -2.0F, 7.0F, 2.0F, 4.0F, new CubeDeformation(0.1F)), PartPose.ZERO);
        skirt.addOrReplaceChild("panel_0", CubeListBuilder.create()
                .texOffs(38, 46).addBox(-3.0F, 0.0F, -0.5F, 6.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(-0.0F, 10.5F, -1.8F, -0.42F, 0.0F, 0.0F));
        skirt.addOrReplaceChild("panel_1", CubeListBuilder.create()
                .texOffs(52, 46).addBox(-2.0F, 0.0F, -0.5F, 4.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(-2.19F, 10.5F, -1.27F, -0.42F, 0.79F, 0.0F));
        skirt.addOrReplaceChild("panel_2", CubeListBuilder.create()
                .texOffs(62, 46).addBox(-2.0F, 0.0F, -0.5F, 4.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(-3.1F, 10.5F, -0.0F, -0.42F, 1.57F, 0.0F));
        skirt.addOrReplaceChild("panel_3", CubeListBuilder.create()
                .texOffs(72, 46).addBox(-2.0F, 0.0F, -0.5F, 4.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(-2.19F, 10.5F, 1.27F, -0.42F, 2.36F, 0.0F));
        skirt.addOrReplaceChild("panel_4", CubeListBuilder.create()
                .texOffs(82, 46).addBox(-3.0F, 0.0F, -0.5F, 6.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(-0.0F, 10.5F, 1.8F, -0.42F, 3.14F, 0.0F));
        skirt.addOrReplaceChild("panel_5", CubeListBuilder.create()
                .texOffs(96, 46).addBox(-2.0F, 0.0F, -0.5F, 4.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(2.19F, 10.5F, 1.27F, -0.42F, 3.93F, 0.0F));
        skirt.addOrReplaceChild("panel_6", CubeListBuilder.create()
                .texOffs(106, 46).addBox(-2.0F, 0.0F, -0.5F, 4.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(3.1F, 10.5F, 0.0F, -0.42F, 4.71F, 0.0F));
        skirt.addOrReplaceChild("panel_7", CubeListBuilder.create()
                .texOffs(116, 46).addBox(-2.0F, 0.0F, -0.5F, 4.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(2.19F, 10.5F, -1.27F, -0.42F, 5.5F, 0.0F));
        head.addOrReplaceChild("right_lock", CubeListBuilder.create()
                .texOffs(20, 28).addBox(-5.0F, -7.0F, -4.6F, 1.0F, 7.0F, 2.0F), PartPose.ZERO);
        head.addOrReplaceChild("left_lock", CubeListBuilder.create()
                .texOffs(26, 28).addBox(4.0F, -7.0F, -4.6F, 1.0F, 7.0F, 2.0F), PartPose.ZERO);
        PartDefinition hairTwintails = head.addOrReplaceChild("hair_twintails", CubeListBuilder.create(), PartPose.ZERO);
        hairTwintails.addOrReplaceChild("right_twintail", CubeListBuilder.create()
                .texOffs(8, 58).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 1.0F, 2.0F)
                .texOffs(32, 28).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F)
                .texOffs(44, 28).addBox(-2.0F, 6.0F, -1.5F, 4.0F, 6.0F, 3.0F)
                .texOffs(28, 52).addBox(-1.5F, 12.0F, -1.0F, 3.0F, 3.0F, 2.0F)
                .texOffs(16, 58).addBox(-1.0F, 15.0F, -0.5F, 2.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(-4.6F, -6.5F, 1.0F, 0.0F, 0.0F, 0.22F));
        hairTwintails.addOrReplaceChild("left_twintail", CubeListBuilder.create()
                .texOffs(22, 58).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 1.0F, 2.0F)
                .texOffs(58, 28).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F)
                .texOffs(70, 28).addBox(-2.0F, 6.0F, -1.5F, 4.0F, 6.0F, 3.0F)
                .texOffs(38, 52).addBox(-1.5F, 12.0F, -1.0F, 3.0F, 3.0F, 2.0F)
                .texOffs(30, 58).addBox(-1.0F, 15.0F, -0.5F, 2.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(4.6F, -6.5F, 1.0F, 0.0F, 0.0F, -0.22F));
        head.addOrReplaceChild("hair_long", CubeListBuilder.create()
                .texOffs(32, 16).addBox(-4.5F, 0.0F, -1.0F, 9.0F, 8.0F, 3.0F)
                .texOffs(84, 28).addBox(-4.0F, 8.0F, -0.5F, 8.0F, 6.0F, 2.0F)
                .texOffs(36, 58).addBox(-3.0F, 14.0F, 0.0F, 6.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -5.0F, 3.5F, 0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("hair_bob", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-5.0F, -5.0F, -1.5F, 10.0F, 6.0F, 6.0F), PartPose.ZERO);
        head.addOrReplaceChild("hair_pony", CubeListBuilder.create()
                .texOffs(50, 58).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 1.0F, 2.0F)
                .texOffs(56, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F)
                .texOffs(104, 28).addBox(-2.0F, 7.0F, -1.5F, 4.0F, 5.0F, 3.0F)
                .texOffs(48, 52).addBox(-1.0F, 12.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(4.4F, -6.5F, 1.5F, 0.0F, 0.0F, -0.3F));
        head.addOrReplaceChild("hair_bow", CubeListBuilder.create()
                .texOffs(112, 52).addBox(-2.5F, -1.5F, -0.5F, 5.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(3.5F, -8.2F, -2.0F, 0.0F, 0.0F, 0.2F));
        PartDefinition earsCat = head.addOrReplaceChild("ears_cat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition earsFox = head.addOrReplaceChild("ears_fox", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition earsBunny = head.addOrReplaceChild("ears_bunny", CubeListBuilder.create(), PartPose.ZERO);
        earsCat.addOrReplaceChild("right_cat_ear", CubeListBuilder.create()
                .texOffs(106, 58).addBox(-2.0F, -1.0F, -0.5F, 4.0F, 1.0F, 1.0F)
                .texOffs(116, 58).addBox(-1.5F, -2.0F, -0.5F, 3.0F, 1.0F, 1.0F)
                .texOffs(0, 61).addBox(-1.0F, -3.0F, -0.5F, 2.0F, 1.0F, 1.0F)
                .texOffs(6, 61).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-2.6F, -8.3F, -0.5F, 0.0F, 0.0F, -0.2F));
        earsFox.addOrReplaceChild("right_fox_ear", CubeListBuilder.create()
                .texOffs(58, 58).addBox(-2.0F, -1.0F, -1.0F, 4.0F, 1.0F, 2.0F)
                .texOffs(70, 58).addBox(-2.0F, -2.0F, -1.0F, 4.0F, 1.0F, 2.0F)
                .texOffs(10, 61).addBox(-1.5F, -3.0F, -0.5F, 3.0F, 1.0F, 1.0F)
                .texOffs(18, 61).addBox(-1.0F, -4.0F, -0.5F, 2.0F, 1.0F, 1.0F)
                .texOffs(24, 61).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-2.6F, -8.3F, -0.5F, 0.0F, 0.0F, -0.3F));
        PartDefinition rightBunnyEar = earsBunny.addOrReplaceChild("right_bunny_ear", CubeListBuilder.create()
                .texOffs(0, 52).addBox(-1.5F, -5.0F, -0.5F, 3.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(-2.3F, -8.3F, -0.5F, 0.0F, 0.0F, -0.1F));
        rightBunnyEar.addOrReplaceChild("right_bunny_tip", CubeListBuilder.create()
                .texOffs(56, 52).addBox(-1.5F, -4.0F, -0.5F, 3.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        earsCat.addOrReplaceChild("left_cat_ear", CubeListBuilder.create()
                .texOffs(28, 61).addBox(-2.0F, -1.0F, -0.5F, 4.0F, 1.0F, 1.0F)
                .texOffs(38, 61).addBox(-1.5F, -2.0F, -0.5F, 3.0F, 1.0F, 1.0F)
                .texOffs(46, 61).addBox(-1.0F, -3.0F, -0.5F, 2.0F, 1.0F, 1.0F)
                .texOffs(52, 61).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(2.6F, -8.3F, -0.5F, 0.0F, 0.0F, 0.2F));
        earsFox.addOrReplaceChild("left_fox_ear", CubeListBuilder.create()
                .texOffs(82, 58).addBox(-2.0F, -1.0F, -1.0F, 4.0F, 1.0F, 2.0F)
                .texOffs(94, 58).addBox(-2.0F, -2.0F, -1.0F, 4.0F, 1.0F, 2.0F)
                .texOffs(56, 61).addBox(-1.5F, -3.0F, -0.5F, 3.0F, 1.0F, 1.0F)
                .texOffs(64, 61).addBox(-1.0F, -4.0F, -0.5F, 2.0F, 1.0F, 1.0F)
                .texOffs(70, 61).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(2.6F, -8.3F, -0.5F, 0.0F, 0.0F, 0.3F));
        PartDefinition leftBunnyEar = earsBunny.addOrReplaceChild("left_bunny_ear", CubeListBuilder.create()
                .texOffs(8, 52).addBox(-1.5F, -5.0F, -0.5F, 3.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(2.3F, -8.3F, -0.5F, 0.0F, 0.0F, 0.1F));
        leftBunnyEar.addOrReplaceChild("left_bunny_tip", CubeListBuilder.create()
                .texOffs(64, 52).addBox(-1.5F, -4.0F, -0.5F, 3.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 1.0F, 0.0F, 0.0F));
        PartDefinition tailCat = body.addOrReplaceChild("tail_cat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition tailCat0 = tailCat.addOrReplaceChild("tail_cat_0", CubeListBuilder.create()
                .texOffs(72, 52).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 9.5F, 1.2F, 1.9F, 0.0F, 0.0F));
        PartDefinition tailCat1 = tailCat0.addOrReplaceChild("tail_cat_1", CubeListBuilder.create()
                .texOffs(80, 52).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 2.5F, 0.0F, 0.3F, 0.0F, 0.0F));
        PartDefinition tailCat2 = tailCat1.addOrReplaceChild("tail_cat_2", CubeListBuilder.create()
                .texOffs(88, 52).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 2.5F, 0.0F, 0.3F, 0.0F, 0.0F));
        PartDefinition tailCat3 = tailCat2.addOrReplaceChild("tail_cat_3", CubeListBuilder.create()
                .texOffs(96, 52).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 2.5F, 0.0F, 0.35F, 0.0F, 0.0F));
        tailCat3.addOrReplaceChild("tail_cat_4", CubeListBuilder.create()
                .texOffs(104, 52).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 2.5F, 0.0F, 0.45F, 0.0F, 0.0F));
        PartDefinition tailFox = body.addOrReplaceChild("tail_fox", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition tailFox0 = tailFox.addOrReplaceChild("tail_fox_0", CubeListBuilder.create()
                .texOffs(32, 38).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 9.5F, 1.2F, 1.7F, 0.0F, 0.0F));
        PartDefinition tailFox1 = tailFox0.addOrReplaceChild("tail_fox_1", CubeListBuilder.create()
                .texOffs(92, 16).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 3.5F, 0.0F, 0.2F, 0.0F, 0.0F));
        PartDefinition tailFox2 = tailFox1.addOrReplaceChild("tail_fox_2", CubeListBuilder.create()
                .texOffs(0, 28).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 4.5F, 0.0F, 0.2F, 0.0F, 0.0F));
        tailFox2.addOrReplaceChild("tail_fox_3", CubeListBuilder.create()
                .texOffs(16, 52).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 4.5F, 0.0F, 0.3F, 0.0F, 0.0F));
        PartDefinition tailBunny = body.addOrReplaceChild("tail_bunny", CubeListBuilder.create(), PartPose.ZERO);
        tailBunny.addOrReplaceChild("tail_bunny_0", CubeListBuilder.create()
                .texOffs(44, 38).addBox(-2.0F, -2.0F, -1.0F, 4.0F, 4.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 9.5F, 2.0F, 0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("blink", CubeListBuilder.create()
                .texOffs(0, 38).addBox(-4.0F, -8.0F, -4.05F, 8.0F, 8.0F, 0.0F), PartPose.ZERO);
        head.addOrReplaceChild("happy", CubeListBuilder.create()
                .texOffs(16, 38).addBox(-4.0F, -8.0F, -4.05F, 8.0F, 8.0F, 0.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    public void setupAnim(Femboy femboy, float limbSwing, float limbAmount, float age, float headYaw, float headPitch) {
        super.setupAnim(femboy, limbSwing, limbAmount, age, headYaw, headPitch);
        Look look = looks.get(Math.floorMod(femboy.getVariant(), looks.size()));
        toggled.forEach(part -> part.visible = false);
        look.parts.forEach(part -> part.visible = true);

        // How far into the delighted reaction to a headpat, easing in and out over a few ticks
        int patTicks = femboy.getPatTicks();
        float delight = Mth.clamp(Math.min(patTicks, Femboy.PAT_REACTION_TICKS - patTicks) / (float) Femboy.PAT_EASE_TICKS, 0.0F, 1.0F);
        float seed = femboy.getId() * 37.0F;
        float time = age + seed;
        float walking = Mth.clamp(limbAmount, 0.0F, 1.0F);

        poseStance(walking, delight);
        poseHead(time, delight);
        poseHair(limbSwing, walking, time);
        poseEars(time, delight);
        poseTail(look.tailRoot, limbSwing, walking, time, delight);
        poseSkirt(limbSwing, walking);

        happy.visible = patTicks > 0;
        blink.visible = !happy.visible && Math.floorMod((int) time, BLINK_PERIOD) < BLINK_TICKS;
        // The vanilla code already copied the head to the hat, before the tilt was added
        hat.copyFrom(head);
    }

    /** Toes turned in when standing still, and both hands up by the face when delighted. */
    private void poseStance(float walking, float delight) {
        float still = 1.0F - walking;
        rightLeg.yRot = -0.14F * still;
        leftLeg.yRot = 0.14F * still;
        rightLeg.zRot = -0.03F * still;
        leftLeg.zRot = 0.03F * still;
        rightArm.zRot += 0.09F;
        leftArm.zRot -= 0.09F;
        rightArm.xRot = Mth.lerp(delight, rightArm.xRot, -2.3F);
        leftArm.xRot = Mth.lerp(delight, leftArm.xRot, -2.3F);
        rightArm.yRot = Mth.lerp(delight, rightArm.yRot, -0.5F);
        leftArm.yRot = Mth.lerp(delight, leftArm.yRot, 0.5F);
    }

    private void poseHead(float time, float delight) {
        head.zRot = Mth.sin(time * 0.03F) * 0.06F + delight * Mth.sin(time * 0.5F) * 0.12F;
    }

    /** Long hair hangs from the head but keeps pointing down when the head tips, and lifts a little when walking. */
    private void poseHair(float limbSwing, float walking, float time) {
        float sway = Mth.sin(time * 0.05F) * 0.03F + Mth.sin(limbSwing * WALK_FREQUENCY) * walking * 0.06F;
        float hang = -head.xRot + walking * 0.25F;
        longHair.xRot = hang;
        longHair.zRot = sway;
        pony.xRot = hang;
        pony.zRot = -PONY_SPLAY + sway;
        rightTwintail.xRot = hang;
        rightTwintail.zRot = TWINTAIL_SPLAY + sway;
        leftTwintail.xRot = hang;
        leftTwintail.zRot = -TWINTAIL_SPLAY + sway;
    }

    /** The odd twitch, in turn, and laid out flat when delighted. */
    private void poseEars(float time, float delight) {
        for (int i = 0; i < ears.length; i++) {
            float side = Math.signum(earTilts[i]);
            float wave = Mth.sin(time * 0.11F + i * 2.0F);
            float twitch = wave > 0.97F ? (wave - 0.97F) * 6.0F : 0.0F;
            ears[i].zRot = earTilts[i] + side * (twitch + delight * 0.6F);
        }
    }

    private static void poseTail(ModelPart tailRoot, float limbSwing, float walking, float time, float delight) {
        float lazy = Mth.sin(time * 0.08F + limbSwing * 0.5F) * (0.15F + walking * 0.3F);
        float wag = Mth.sin(time * 0.9F) * 0.55F;
        tailRoot.zRot = Mth.lerp(delight, lazy, wag);
    }

    /** Panels on the side a leg swings towards get pushed out by it, and the whole skirt swishes with the step. */
    private void poseSkirt(float limbSwing, float walking) {
        float forward = Math.max(0.0F, -Math.min(rightLeg.xRot, leftLeg.xRot));
        float backward = Math.max(0.0F, Math.max(rightLeg.xRot, leftLeg.xRot));
        for (int i = 0; i < PANELS; i++) {
            // Panel 0 faces the front, panel 4 the back
            float facing = Mth.cos(i * Mth.TWO_PI / PANELS);
            float push = facing > 0.0F ? facing * forward : -facing * backward;
            panels[i].xRot = -PANEL_FLARE - push * PANEL_SWING;
        }
        skirt.zRot = Mth.sin(limbSwing * WALK_FREQUENCY) * walking * 0.05F;
    }

    private record Look(List<ModelPart> parts, ModelPart tailRoot) {}
}
