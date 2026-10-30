package ch.rere232.skofcraft.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Shared projectile launching for aircraft. */
final class AircraftWeapons {
    private AircraftWeapons() {}

    /** Fires from the muzzle toward the point the pilot is looking at, so shots meet at the crosshair. */
    static void launch(Level level, Projectile projectile, Vec3 muzzle, LivingEntity pilot, float velocity, float inaccuracy) {
        Vec3 aim = pilot.getEyePosition().add(pilot.getLookAngle().scale(80.0D)).subtract(muzzle).normalize();
        projectile.setPos(muzzle.x, muzzle.y, muzzle.z);
        projectile.shoot(aim.x, aim.y, aim.z, velocity, inaccuracy);
        level.addFreshEntity(projectile);
    }
}
