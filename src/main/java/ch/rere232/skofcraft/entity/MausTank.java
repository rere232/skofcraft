package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** The Panzer VIII Maus: slow, enormous and very hard to hurt. */
public class MausTank extends Tank {
    public static final Spec SPEC = new Spec(SkofcraftItems.MAUS, 200.0F, 4.0F, AircraftFuel.PER_CANISTER * 3, 10.0F, 1.0F,
            // Top speed is about the 20 km/h the real one managed
            new Mobility(0.28D, 0.12D, 0.006D, 0.02D, 1.5F),
            new Gun(20 * 8, 5.0F, 1.0F, 2.5F, 0.5D, 3.0D, 7.2D),
            new Layout(new double[] {0.5D, 2.75D}, new double[][] {
                    // Four on the engine deck and two on the hull in front of the turret
                    {-2.0D, 0.7D, 2.0D}, {-2.0D, -0.7D, 2.0D},
                    {-3.2D, 0.7D, 2.0D}, {-3.2D, -0.7D, 2.0D},
                    {2.8D, 1.0D, 1.65D}, {2.8D, -1.0D, 1.65D}
            }, 4.0D, 2.2D, 9.0D));

    public MausTank(EntityType<? extends MausTank> type, Level level) {
        super(type, level);
    }

    @Override
    public Spec spec() {
        return SPEC;
    }

    @Override
    protected void onDriverBoarded(Entity driver) {
        SkofcraftAdvancements.award(driver, SkofcraftAdvancements.ONE_HUNDRED_EIGHTY_EIGHT_TONS);
    }
}
