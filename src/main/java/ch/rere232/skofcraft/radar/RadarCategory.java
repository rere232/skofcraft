package ch.rere232.skofcraft.radar;

import ch.rere232.skofcraft.entity.AircraftCarrier;
import ch.rere232.skofcraft.entity.DeliveryPlane;
import ch.rere232.skofcraft.entity.PilotedAircraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import org.jetbrains.annotations.Nullable;

/** What a radar contact is, which sets its blip colour and which filters show it. */
public enum RadarCategory {
    AIRCRAFT(0xFFFF5050),
    SHIP(0xFF50A0FF),
    PLAYER(0xFF50FFFF),
    HOSTILE(0xFFFFA030),
    CREATURE(0xFF60E060);

    public final int color;

    RadarCategory(int color) {
        this.color = color;
    }

    public String key() {
        return "screen.skofcraft.radar.category." + name().toLowerCase(java.util.Locale.ROOT);
    }

    /** The category of something the radar can see, or null if it does not show up (items, projectiles, the invisible). */
    @Nullable
    public static RadarCategory of(Entity entity) {
        if (!entity.isAlive()) {
            return null;
        }
        if (entity instanceof PilotedAircraft || entity instanceof DeliveryPlane || entity instanceof EnderDragon) {
            return AIRCRAFT;
        }
        if (entity instanceof AircraftCarrier || entity instanceof Boat) {
            return SHIP;
        }
        if (entity.isInvisible()) {
            return null;
        }
        if (entity instanceof Player player) {
            return player.isSpectator() ? null : PLAYER;
        }
        if (entity instanceof Enemy) {
            return HOSTILE;
        }
        if (entity instanceof LivingEntity && !(entity instanceof ArmorStand)) {
            return CREATURE;
        }
        return null;
    }

    public static RadarCategory byId(int id) {
        RadarCategory[] values = values();
        return values[Math.floorMod(id, values.length)];
    }
}
