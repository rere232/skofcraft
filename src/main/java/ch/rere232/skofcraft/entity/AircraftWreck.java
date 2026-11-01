package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Parts thrown out of a destroyed aircraft. Called after the explosion so the blast does not destroy them. */
public final class AircraftWreck {
    private AircraftWreck() {}

    /** Call before ejecting the passengers: everyone still aboard gets the Scrap Metal advancement. */
    public static void awardCrew(Entity aircraft) {
        for (Entity passenger : aircraft.getPassengers()) {
            SkofcraftAdvancements.award(passenger, SkofcraftAdvancements.SCRAP_METAL);
        }
    }

    public static void scatterF14(Entity jet) {
        Level level = jet.level();
        RandomSource random = level.getRandom();
        drop(jet, random, Items.IRON_NUGGET, 6, 16, 1.0F);
        drop(jet, random, SkofcraftItems.GEAR.get(), 2, 5, 1.0F);
        drop(jet, random, SkofcraftItems.AIRCRAFT_PLATE.get(), 3, 8, 1.0F);
        drop(jet, random, SkofcraftItems.TURBINE_BLADE.get(), 1, 3, 0.6F);
        drop(jet, random, SkofcraftItems.AVIONICS.get(), 1, 1, 0.2F);
        drop(jet, random, SkofcraftItems.JET_ENGINE.get(), 1, 1, 0.08F);
    }

    public static void scatterApache(Entity helicopter) {
        Level level = helicopter.level();
        RandomSource random = level.getRandom();
        drop(helicopter, random, Items.IRON_NUGGET, 5, 12, 1.0F);
        drop(helicopter, random, SkofcraftItems.GEAR.get(), 2, 5, 1.0F);
        drop(helicopter, random, SkofcraftItems.AIRCRAFT_PLATE.get(), 2, 6, 1.0F);
        drop(helicopter, random, SkofcraftItems.ROTOR_BLADE.get(), 1, 2, 0.5F);
        drop(helicopter, random, SkofcraftItems.TURBINE_BLADE.get(), 1, 2, 0.4F);
        drop(helicopter, random, SkofcraftItems.AVIONICS.get(), 1, 1, 0.2F);
        drop(helicopter, random, SkofcraftItems.JET_ENGINE.get(), 1, 1, 0.08F);
    }

    private static void drop(Entity wreck, RandomSource random, Item item, int min, int max, float chance) {
        if (random.nextFloat() >= chance) {
            return;
        }
        int count = min + random.nextInt(max - min + 1);
        // One entity per item so the debris spreads out
        for (int i = 0; i < count; i++) {
            ItemEntity debris = new ItemEntity(wreck.level(), wreck.getX(), wreck.getY() + 1.0D, wreck.getZ(), new ItemStack(item));
            debris.setDeltaMovement(random.nextGaussian() * 0.25D, 0.3D + random.nextDouble() * 0.35D, random.nextGaussian() * 0.25D);
            debris.setDefaultPickUpDelay();
            wreck.level().addFreshEntity(debris);
        }
    }
}
