package ch.rere232.skofcraft.advancement;

import ch.rere232.skofcraft.SkofcraftMod;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Grants advancements whose only criterion is "minecraft:impossible", for events no vanilla trigger covers. */
public final class SkofcraftAdvancements {
    public static final String REFUEL = "refuel";
    public static final String RUNNING_ON_FUMES = "running_on_fumes";
    public static final String SCRAP_METAL = "scrap_metal";
    public static final String NUCLEAR_NAVY = "nuclear_navy";
    public static final String ANCHORS_AWEIGH = "anchors_aweigh";
    public static final String CAT_SHOT = "cat_shot";
    public static final String TRAP = "trap";
    public static final String MELTDOWN = "meltdown";
    public static final String RADAR_CONTACT = "radar_contact";
    public static final String SATELLITE_LAUNCH = "satellite_launch";
    public static final String ORBITAL_STRIKE = "orbital_strike";
    public static final String GIGA_ICBM = "giga_icbm";
    public static final String FULL_OUTFIT = "full_outfit";
    public static final String FULLY_KITTED = "fully_kitted";

    private SkofcraftAdvancements() {}

    public static void award(Entity entity, String path) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        Advancement advancement = player.server.getAdvancements().getAdvancement(ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, path));
        if (advancement == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }
}
