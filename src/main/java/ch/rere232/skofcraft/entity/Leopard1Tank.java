package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The Leopard 1: built on the idea that no armor could stop the shells of its day, so it might as well be fast. Quick
 * in every way, long range, and the only tank here a machine gun can hurt.
 */
public class Leopard1Tank extends Tank {
    public static final Spec SPEC = new Spec(SkofcraftItems.LEOPARD_1, 90.0F, 2.0F, AircraftFuel.PER_CANISTER * 4, 5.0F, 1.3F,
            // Top speed is the real 65 km/h
            new Mobility(0.9D, 0.3D, 0.02D, 0.05D, 3.0F),
            new Gun(20 * 5, 4.0F, 1.15F, 6.0F, 0.625D, 2.125D, 5.375D),
            new Layout(new double[] {0.6D, 1.75D}, new double[][] {
                    {-1.6D, 0.7D, 1.15D}, {-1.6D, -0.7D, 1.15D},
                    {2.75D, 0.9D, 0.9D}, {2.75D, -0.9D, 0.9D}
            }, 2.8D, 2.0D, 7.0D));

    public Leopard1Tank(EntityType<? extends Leopard1Tank> type, Level level) {
        super(type, level);
    }

    @Override
    public Spec spec() {
        return SPEC;
    }

    @Override
    protected void onDriverBoarded(Entity driver) {
        SkofcraftAdvancements.award(driver, SkofcraftAdvancements.SPEED_IS_ARMOR);
    }
}
