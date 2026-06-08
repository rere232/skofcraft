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

    private static final ForgeConfigSpec.DoubleValue TOBACCO_GROWTH_SPEED = BUILDER.comment("Tobacco growth speed multiplier")
            .defineInRange("tobaccoGrowthSpeed", 1.0D, 0.1D, 10.0D);
    private static final ForgeConfigSpec.DoubleValue MACHINE_ENERGY_MULTIPLIER = BUILDER.comment("Machine FE/t multiplier")
            .defineInRange("machineEnergyMultiplier", 1.0D, 0.1D, 10.0D);

    private static final ForgeConfigSpec.IntValue SNUS_DURATION_MINUTES = BUILDER.comment("Snus duration in minutes")
            .defineInRange("snusDurationMinutes", 15, 1, 120);
    private static final ForgeConfigSpec.IntValue POUCH_DURATION_MINUTES = BUILDER.comment("Pouch duration in minutes")
            .defineInRange("pouchDurationMinutes", 15, 1, 120);
    private static final ForgeConfigSpec.IntValue BOX_CAPACITY = BUILDER.comment("Units per box")
            .defineInRange("boxCapacity", 20, 1, 64);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enableAddiction;
    public static boolean enableTolerance;
    public static boolean enableNegativeEffects;
    public static boolean enableVillager;

    public static double tobaccoGrowthSpeed;
    public static double machineEnergyMultiplier;

    public static int snusDurationMinutes;
    public static int pouchDurationMinutes;
    public static int boxCapacity;

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        enableAddiction = ENABLE_ADDICTION.get();
        enableTolerance = ENABLE_TOLERANCE.get();
        enableNegativeEffects = ENABLE_NEGATIVE_EFFECTS.get();
        enableVillager = ENABLE_VILLAGER.get();

        tobaccoGrowthSpeed = TOBACCO_GROWTH_SPEED.get();
        machineEnergyMultiplier = MACHINE_ENERGY_MULTIPLIER.get();

        snusDurationMinutes = SNUS_DURATION_MINUTES.get();
        pouchDurationMinutes = POUCH_DURATION_MINUTES.get();
        boxCapacity = BOX_CAPACITY.get();
    }
}
