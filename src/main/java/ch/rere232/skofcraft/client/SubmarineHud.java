package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.entity.AircraftCarrier;
import ch.rere232.skofcraft.entity.NuclearSubmarine;
import ch.rere232.skofcraft.entity.Torpedo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/**
 * The boat's instruments. Top right, the sonar with the bow pointing up: it always hears ships and torpedoes close by,
 * and an active ping charts everything in the water further out for a few seconds. Bottom left, the boat's state;
 * above the hotbar, the torpedo reload; at the top, the heading.
 */
public final class SubmarineHud {
    private static final int GREEN = 0xFF55FF88;
    private static final int GREEN_DIM = 0xAA2E9E50;
    private static final int AMBER = 0xFFFFB347;
    private static final int RED = 0xFFFF5555;
    private static final int BLUE = 0xFF55BBFF;
    private static final int PANEL = 0x66000000;
    private static final int MARGIN = 6;
    private static final int SONAR_RADIUS = 40;
    /** Clears the equipment icons in the top right corner. */
    private static final int SONAR_TOP = 28;
    /** Clears the action bar, which is where the boat's messages and the dismount hint appear. */
    private static final int RELOAD_BAR_RISE = 80;
    private static final double PASSIVE_RANGE = 64.0D;
    private static final double ACTIVE_RANGE = 128.0D;
    private static final double TORPEDO_WARNING_RANGE = 64.0D;
    private static final double TORPEDO_WARNING_AIM = 0.9D;
    private static final int RING_POINTS = 96;
    /** Scaled to the boat, which is about 3.5 times shorter than the real one. */
    private static final double METRES_PER_BLOCK = 3.5D;
    private static final double KNOTS_PER_BLOCK_PER_TICK = 40.0D;
    private static final int RELOAD_BAR_WIDTH = 90;
    private static final int STATUS_BAR_WIDTH = 50;
    private static final int THROTTLE_BAR_HEIGHT = 36;

    private SubmarineHud() {}

    public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.player != null && mc.player.getVehicle() instanceof NuclearSubmarine boat) || mc.options.hideGui || mc.level == null) {
            return;
        }
        int cx = width / 2;
        drawSonar(g, mc, boat, width, partialTick);
        drawStatus(g, mc, boat, height);
        drawHeading(g, mc, boat, cx);
        if (AircraftClient.piloted() == boat) {
            drawThrottle(g, boat, height);
            drawReload(g, mc, boat, cx, height);
        }
        if (boat.isLaunching()) {
            g.drawCenteredString(mc.font, Component.translatable("hud.skofcraft.sub.launching"), cx, height / 2 - 30, flash(mc) ? RED : AMBER);
        } else if (torpedoInbound(mc, boat)) {
            g.drawCenteredString(mc.font, Component.translatable("hud.skofcraft.sub.torpedo"), cx, height / 2 - 30, flash(mc) ? RED : AMBER);
        }
    }

    private static boolean flash(Minecraft mc) {
        return mc.level.getGameTime() / 5 % 2 == 0;
    }

    private static boolean torpedoInbound(Minecraft mc, NuclearSubmarine boat) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Torpedo torpedo && torpedo.distanceTo(boat) < TORPEDO_WARNING_RANGE) {
                Vec3 toBoat = boat.position().subtract(torpedo.position()).normalize();
                if (torpedo.getDeltaMovement().normalize().dot(toBoat) > TORPEDO_WARNING_AIM) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void drawSonar(GuiGraphics g, Minecraft mc, NuclearSubmarine boat, int width, float partialTick) {
        int cx = width - MARGIN - SONAR_RADIUS - 2;
        int cy = SONAR_TOP + SONAR_RADIUS + 2;
        g.fill(cx - SONAR_RADIUS - 2, cy - SONAR_RADIUS - 2, cx + SONAR_RADIUS + 3, cy + SONAR_RADIUS + 3, PANEL);
        ring(g, cx, cy, SONAR_RADIUS, GREEN_DIM);
        ring(g, cx, cy, SONAR_RADIUS / 2, GREEN_DIM);
        g.fill(cx, cy - 3, cx + 1, cy + 4, GREEN);

        long sincePing = mc.level.getGameTime() - boat.getLastPing();
        boolean charting = sincePing >= 0L && sincePing < NuclearSubmarine.SONAR_CHART_TICKS;
        double range = charting ? ACTIVE_RANGE : PASSIVE_RANGE;
        if (charting) {
            // The ping going out
            float front = (sincePing + partialTick) / 30.0F;
            if (front < 1.0F) {
                ring(g, cx, cy, (int) (SONAR_RADIUS * front), BLUE);
            }
        }
        double yaw = Math.toRadians(boat.getViewYRot(partialTick));
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        for (Entity entity : mc.level.entitiesForRendering()) {
            int color = blipColor(entity, boat, charting);
            if (color == 0) {
                continue;
            }
            double dx = entity.getX() - boat.getX();
            double dz = entity.getZ() - boat.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > range || distance < 1.0D) {
                continue;
            }
            double along = dx * fx + dz * fz;
            double right = dx * -fz + dz * fx;
            int bx = cx + (int) (right / range * SONAR_RADIUS);
            int by = cy - (int) (along / range * SONAR_RADIUS);
            g.fill(bx - 1, by - 1, bx + 1, by + 1, color);
        }
        g.drawCenteredString(mc.font, Component.translatable(charting ? "hud.skofcraft.sub.active" : "hud.skofcraft.sub.passive"),
                cx, cy + SONAR_RADIUS + 5, charting ? BLUE : GREEN);
    }

    private static int blipColor(Entity entity, NuclearSubmarine boat, boolean charting) {
        if (entity == boat || entity.getVehicle() == boat) {
            return 0;
        }
        if (entity instanceof Torpedo) {
            return RED;
        }
        if (entity instanceof AircraftCarrier || entity instanceof Boat) {
            return BLUE;
        }
        if (entity instanceof NuclearSubmarine) {
            return charting ? RED : 0;
        }
        return charting && entity instanceof LivingEntity && entity.isInWater() ? GREEN : 0;
    }

    private static void ring(GuiGraphics g, int cx, int cy, int radius, int color) {
        for (int i = 0; i < RING_POINTS; i++) {
            float angle = Mth.TWO_PI * i / RING_POINTS;
            int x = cx + (int) (Mth.cos(angle) * radius);
            int y = cy + (int) (Mth.sin(angle) * radius);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static void drawStatus(GuiGraphics g, Minecraft mc, NuclearSubmarine boat, int height) {
        int x = MARGIN + 6;
        int y = height - MARGIN - 62;
        g.fill(x - 4, y - 4, x + STATUS_BAR_WIDTH + 44, y + 58, PANEL);
        int knots = (int) Math.round(boat.shownSpeed * KNOTS_PER_BLOCK_PER_TICK);
        g.drawString(mc.font, Component.translatable("hud.skofcraft.sub.speed", knots), x, y, GREEN, true);
        int depth = (int) Math.round(boat.depth() * METRES_PER_BLOCK);
        g.drawString(mc.font, depth > 0 ? Component.translatable("hud.skofcraft.sub.depth", depth) : Component.translatable("hud.skofcraft.sub.surfaced"), x, y + 11, BLUE, true);
        float hull = boat.getHealth() / boat.getMaxHealth();
        int hullColor = hull < 0.25F ? RED : GREEN;
        g.drawString(mc.font, Component.translatable("hud.skofcraft.apache.hull"), x, y + 22, hullColor, true);
        g.fill(x + 30, y + 24, x + 30 + STATUS_BAR_WIDTH, y + 28, PANEL);
        g.fill(x + 30, y + 24, x + 30 + (int) (STATUS_BAR_WIDTH * Mth.clamp(hull, 0.0F, 1.0F)), y + 28, hullColor);
        g.drawString(mc.font, Component.translatable("hud.skofcraft.sub.reactor"), x, y + 33, GREEN, true);
        g.drawString(mc.font, Component.translatable("hud.skofcraft.sub.tubes", boat.getMissileCount(), NuclearSubmarine.MISSILE_TUBES), x, y + 44,
                boat.getMissileCount() > 0 ? AMBER : GREEN_DIM, true);
    }

    private static void drawHeading(GuiGraphics g, Minecraft mc, NuclearSubmarine boat, int cx) {
        int heading = Math.floorMod(Math.round(boat.getYRot()) + 180, 360);
        g.drawCenteredString(mc.font, String.format("%03d°", heading), cx, MARGIN + 2, GREEN);
    }

    private static void drawThrottle(GuiGraphics g, NuclearSubmarine boat, int height) {
        int x = MARGIN + 6 + STATUS_BAR_WIDTH + 48;
        int middle = height - MARGIN - 34;
        g.fill(x, middle - THROTTLE_BAR_HEIGHT / 2, x + 4, middle + THROTTLE_BAR_HEIGHT / 2, PANEL);
        g.fill(x - 1, middle, x + 5, middle + 1, GREEN_DIM);
        int order = (int) (boat.getThrottle() * THROTTLE_BAR_HEIGHT / 2);
        if (order > 0) {
            g.fill(x, middle - order, x + 4, middle, GREEN);
        } else if (order < 0) {
            g.fill(x, middle, x + 4, middle - order, AMBER);
        }
    }

    private static void drawReload(GuiGraphics g, Minecraft mc, NuclearSubmarine boat, int cx, int height) {
        long sinceShot = mc.level.getGameTime() - AircraftClient.lastSecondaryTick;
        float reload = Mth.clamp((float) sinceShot / boat.secondaryCooldownTicks(), 0.0F, 1.0F);
        boolean ready = reload >= 1.0F;
        int color = ready ? GREEN : AMBER;
        int barX = cx - RELOAD_BAR_WIDTH / 2;
        int barY = height - RELOAD_BAR_RISE;
        g.drawCenteredString(mc.font, Component.translatable(ready ? "hud.skofcraft.sub.torpedo_ready" : "hud.skofcraft.sub.torpedo_loading"), cx, barY - 10, color);
        g.fill(barX, barY, barX + RELOAD_BAR_WIDTH, barY + 2, PANEL);
        g.fill(barX, barY, barX + (int) (RELOAD_BAR_WIDTH * reload), barY + 2, color);
    }
}
