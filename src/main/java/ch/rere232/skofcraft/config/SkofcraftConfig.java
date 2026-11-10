package ch.rere232.skofcraft.config;

import ch.rere232.skofcraft.SkofcraftMod;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class SkofcraftConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENABLE_ADDICTION = BUILDER.comment("Enable addiction penalties")
            .define("enableAddiction", true);
    private static final ForgeConfigSpec.BooleanValue ENABLE_TOLERANCE = BUILDER.comment("Enable tolerance system")
            .define("enableTolerance", true);
    private static final ForgeConfigSpec.BooleanValue ENABLE_NEGATIVE_EFFECTS = BUILDER.comment("Enable negative status effects")
            .define("enableNegativeEffects", true);
    private static final ForgeConfigSpec.BooleanValue ENABLE_VILLAGER = BUILDER.comment("Enable tobacconist villager logic")
            .define("enableVillager", true);
    private static final ForgeConfigSpec.BooleanValue ENABLE_MANDATORY_HR = BUILDER.comment("Send HR after three war crimes")
            .define("enableMandatoryHr", true);
    private static final ForgeConfigSpec.BooleanValue ENABLE_CUDDLING = BUILDER.comment("Let players ask each other for a cuddle")
            .define("enableCuddling", true);
    private static final ForgeConfigSpec.BooleanValue ENABLE_SNUS_KINGDOM = BUILDER.comment("Send players to the Snus Kingdom when they overdose on four Extra Strong pouches")
            .define("enableSnusKingdom", true);
    private static final ForgeConfigSpec.BooleanValue ENABLE_PASTEL_PARADISE = BUILDER.comment("Send players to Pastel Paradise when they hug a Blahaj in the full fit with ears and a tail")
            .define("enablePastelParadise", true);

    private static final ForgeConfigSpec.DoubleValue TOBACCO_GROWTH_SPEED = BUILDER.comment("Tobacco growth speed multiplier")
            .defineInRange("tobaccoGrowthSpeed", 1.0D, 0.1D, 10.0D);
    private static final ForgeConfigSpec.DoubleValue SKOFEE_GROWTH_SPEED = BUILDER.comment("SKOFee growth speed multiplier")
            .defineInRange("skofeeGrowthSpeed", 1.0D, 0.1D, 10.0D);
    private static final ForgeConfigSpec.DoubleValue MACHINE_ENERGY_MULTIPLIER = BUILDER.comment("Machine FE/t multiplier")
            .defineInRange("machineEnergyMultiplier", 1.0D, 0.1D, 10.0D);

    private static final ForgeConfigSpec.IntValue SNUS_DURATION_MINUTES = BUILDER.comment("Snus duration in minutes")
            .defineInRange("snusDurationMinutes", 15, 1, 120);
    private static final ForgeConfigSpec.IntValue POUCH_DURATION_MINUTES = BUILDER.comment("Pouch duration in minutes")
            .defineInRange("pouchDurationMinutes", 15, 1, 120);
    private static final ForgeConfigSpec.IntValue CHEW_DURATION_MINUTES = BUILDER.comment("Chewing tobacco duration in minutes")
            .defineInRange("chewDurationMinutes", 30, 1, 240);
    private static final ForgeConfigSpec.IntValue BOX_CAPACITY = BUILDER.comment("Units per box")
            .defineInRange("boxCapacity", 20, 1, 64);
    private static final ForgeConfigSpec.IntValue VAPE_USES = BUILDER.comment("Puffs per filled vape")
            .defineInRange("vapeUses", 30, 1, 200);
    private static final ForgeConfigSpec.IntValue SKOFEE_DURATION_SECONDS = BUILDER.comment("Speed and Haste duration of a cup of SKOFee in seconds")
            .defineInRange("skofeeDurationSeconds", 180, 10, 1200);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enableAddiction;
    public static boolean enableTolerance;
    public static boolean enableNegativeEffects;
    public static boolean enableVillager;
    public static boolean enableMandatoryHr;
    public static boolean enableCuddling = true;
    public static boolean enableSnusKingdom = true;
    public static boolean enablePastelParadise = true;

    public static double tobaccoGrowthSpeed;
    public static double skofeeGrowthSpeed = 1.0D;
    public static double machineEnergyMultiplier;

    public static int snusDurationMinutes;
    public static int pouchDurationMinutes;
    public static int chewDurationMinutes = 30;
    public static int boxCapacity;
    public static int vapeUses = 30;
    public static int skofeeDurationSeconds = 180;

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        // The mod also has a client config (SkofcraftClientConfig), loaded first; only this file's events concern us
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        enableAddiction = ENABLE_ADDICTION.get();
        enableTolerance = ENABLE_TOLERANCE.get();
        enableNegativeEffects = ENABLE_NEGATIVE_EFFECTS.get();
        enableVillager = ENABLE_VILLAGER.get();
        enableMandatoryHr = ENABLE_MANDATORY_HR.get();
        enableCuddling = ENABLE_CUDDLING.get();
        enableSnusKingdom = ENABLE_SNUS_KINGDOM.get();
        enablePastelParadise = ENABLE_PASTEL_PARADISE.get();

        tobaccoGrowthSpeed = TOBACCO_GROWTH_SPEED.get();
        skofeeGrowthSpeed = SKOFEE_GROWTH_SPEED.get();
        machineEnergyMultiplier = MACHINE_ENERGY_MULTIPLIER.get();

        snusDurationMinutes = SNUS_DURATION_MINUTES.get();
        pouchDurationMinutes = POUCH_DURATION_MINUTES.get();
        chewDurationMinutes = CHEW_DURATION_MINUTES.get();
        boxCapacity = BOX_CAPACITY.get();
        vapeUses = VAPE_USES.get();
        skofeeDurationSeconds = SKOFEE_DURATION_SECONDS.get();
    }
}
