package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The Tiger II: heavy armor and a long 88 mm gun, on an engine meant for something lighter. Small arms bounce off, but
 * the turret takes its time and the tank drinks fuel, which is what stopped a lot of the real ones.
 */
public class Tiger2Tank extends Tank {
    public static final Spec SPEC = new Spec(SkofcraftItems.TIGER_2, 150.0F, 4.0F, AircraftFuel.PER_CANISTER * 2, 8.0F, 0.9F,
            // Top speed is about the real 38 km/h
            new Mobility(0.53D, 0.15D, 0.008D, 0.025D, 1.8F),
            new Gun(20 * 6, 4.5F, 1.05F, 1.5F, 0.25D, 2.625D, 6.25D),
            new Layout(new double[] {0.25D, 2.1D}, new double[][] {
                    {-1.5D, 0.7D, 1.65D}, {-1.5D, -0.7D, 1.65D},
                    {-2.75D, 0.7D, 1.65D}, {-2.75D, -0.7D, 1.65D},
                    {2.6D, 1.0D, 1.5D}, {2.6D, -1.0D, 1.5D}
            }, 3.0D, 2.2D, 7.0D));

    public Tiger2Tank(EntityType<? extends Tiger2Tank> type, Level level) {
        super(type, level);
    }

    @Override
    public Spec spec() {
        return SPEC;
    }

    @Override
    protected void onDriverBoarded(Entity driver) {
        SkofcraftAdvancements.award(driver, SkofcraftAdvancements.KING_TIGER);
    }
}
