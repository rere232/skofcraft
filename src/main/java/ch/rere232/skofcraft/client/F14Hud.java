package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.entity.F14Tomcat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** Fighter HUD: pitch ladder, flight data, throttle, stall warning, weapons, hull and fuel. */
public final class F14Hud {
    private static final int GREEN = 0xFF55FF88;
    private static final int GREEN_DIM = 0xFF2E9E50;
    private static final int AMBER = 0xFFFFB347;
    private static final int RED = 0xFFFF5555;
    private static final String[] COMPASS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    private F14Hud() {}

    public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (!(AircraftClient.piloted() instanceof F14Tomcat jet) || mc.options.hideGui) {
            return;
        }
        int cx = width / 2;
        int cy = height / 2;

        if (mc.options.getCameraType().isFirstPerson()) {
            drawCanopy(g, width, height);
        }
        drawLadder(g, mc, cx, cy, height);
        drawReticle(g, cx, cy);
        drawFlightData(g, jet, mc, cx, cy);
        drawWeapons(g, jet, mc, cx, height);
    }

    private static void drawCanopy(GuiGraphics g, int width, int height) {
        g.fillGradient(0, height - height / 8, width, height, 0x00101418, 0xEE101418);
        g.fillGradient(0, 0, width, height / 10, 0xCC101418, 0x00101418);
        g.fillGradient(0, 0, width / 12, height, 0x99101418, 0x00101418);
        g.fillGradient(width - width / 12, 0, width, height, 0x00101418, 0x99101418);
    }

    private static void drawReticle(GuiGraphics g, int cx, int cy) {
        g.fill(cx - 12, cy, cx - 4, cy + 1, GREEN);
        g.fill(cx + 4, cy, cx + 12, cy + 1, GREEN);
        g.fill(cx, cy - 12, cx + 1, cy - 4, GREEN);
        g.fill(cx, cy + 4, cx + 1, cy + 13, GREEN);
        g.fill(cx, cy, cx + 1, cy + 1, GREEN);
    }

    /** Lines every 10 degrees of elevation, positioned from the camera pitch. */
    private static void drawLadder(GuiGraphics g, Minecraft mc, int cx, int cy, int height) {
        float cameraPitch = mc.player.getXRot();
        float pixelsPerDegree = height / 70.0F;
        for (int angle = -60; angle <= 60; angle += 10) {
            int y = Math.round(cy - (angle + cameraPitch) * pixelsPerDegree);
            if (y < height / 6 || y > height - height / 6) {
                continue;
            }
            int half = angle == 0 ? 70 : 24;
            int color = angle == 0 ? GREEN : GREEN_DIM;
            g.fill(cx - half - 20, y, cx - 20, y + 1, color);
            g.fill(cx + 20, y, cx + half + 20, y + 1, color);
            if (angle != 0) {
                int tick = angle > 0 ? 3 : -3;
                g.fill(cx - 20, Math.min(y, y + tick), cx - 19, Math.max(y, y + tick) + 1, color);
                g.fill(cx + 19, Math.min(y, y + tick), cx + 20, Math.max(y, y + tick) + 1, color);
                g.drawString(mc.font, Integer.toString(Math.abs(angle)), cx + half + 24, y - 3, color, false);
            }
        }
    }

    private static void drawFlightData(GuiGraphics g, F14Tomcat jet, Minecraft mc, int cx, int cy) {
        int speed = (int) Math.round(Math.hypot(Math.hypot(jet.getX() - jet.xo, jet.getY() - jet.yo), jet.getZ() - jet.zo) * 20.0D);
        int altitude = (int) Math.floor(jet.getY());
        int heading = Math.floorMod(Math.round(jet.getYRot() + 180.0F), 360);
        String cardinal = COMPASS[Math.floorMod(Math.round(heading / 45.0F), 8)];
        boolean stalling = !jet.onGround() && jet.speed < F14Tomcat.STALL_SPEED;

        g.drawString(mc.font, Component.translatable("hud.skofcraft.f14.spd", speed), cx - 150, cy - 12, GREEN, false);
        g.drawString(mc.font, Component.translatable("hud.skofcraft.f14.alt", altitude), cx + 100, cy - 12, GREEN, false);
        g.drawCenteredString(mc.font, Component.translatable("hud.skofcraft.f14.hdg", heading, cardinal), cx, 14, GREEN);

        // vertical throttle bar
        int barX = cx - 150;
        int top = cy + 4;
        int barH = 46;
        g.fill(barX, top, barX + 5, top + barH, 0xFF0A0C0A);
        int filled = (int) (barH * jet.throttle);
        g.fill(barX, top + barH - filled, barX + 5, top + barH, jet.afterburner ? AMBER : GREEN);
        g.drawString(mc.font, Component.translatable("hud.skofcraft.f14.thr"), barX + 9, top + barH - 8, GREEN_DIM, false);
        if (jet.afterburner) {
            g.drawString(mc.font, Component.translatable("hud.skofcraft.f14.ab"), barX + 9, top, AMBER, false);
        }

        if (stalling && (mc.level != null && mc.level.getGameTime() / 4 % 2 == 0)) {
            g.drawCenteredString(mc.font, Component.translatable("hud.skofcraft.f14.stall"), cx, cy + 40, RED);
        } else if (jet.onGround() && jet.speed < F14Tomcat.TAKEOFF_SPEED) {
            g.drawCenteredString(mc.font, Component.translatable("hud.skofcraft.f14.takeoff"), cx, cy + 40, GREEN_DIM);
        }
    }

    private static void drawWeapons(GuiGraphics g, F14Tomcat jet, Minecraft mc, int cx, int height) {
        long now = mc.level != null ? mc.level.getGameTime() : 0L;
        float reload = Mth.clamp((now - AircraftClient.lastSecondaryTick) / (float) F14Tomcat.MISSILE_COOLDOWN, 0.0F, 1.0F);
        int y = height - 36;

        g.drawString(mc.font, Component.translatable("hud.skofcraft.f14.gun"), cx - 90, y, GREEN, false);
        g.fill(cx - 90, y + 10, cx - 30, y + 13, GREEN);

        int missileColor = reload < 1.0F ? AMBER : GREEN;
        g.drawString(mc.font, Component.translatable("hud.skofcraft.f14.missiles"), cx - 18, y, missileColor, false);
        g.fill(cx - 18, y + 10, cx + 42, y + 13, 0xFF0A0C0A);
        g.fill(cx - 18, y + 10, cx - 18 + (int) (60 * reload), y + 13, missileColor);

        float hull = Mth.clamp(jet.getHealth() / jet.getMaxHealth(), 0.0F, 1.0F);
        int hullColor = hull > 0.5F ? GREEN : (hull > 0.25F ? AMBER : RED);
        g.drawString(mc.font, Component.translatable("hud.skofcraft.f14.hull"), cx + 54, y, hullColor, false);
        g.fill(cx + 54, y + 10, cx + 114, y + 13, 0xFF0A0C0A);
        g.fill(cx + 54, y + 10, cx + 54 + (int) (60 * hull), y + 13, hullColor);

        float fuel = Mth.clamp(jet.getFuel() / (float) jet.getMaxFuel(), 0.0F, 1.0F);
        int fuelColor = fuel > 0.25F ? GREEN : (fuel > 0.1F ? AMBER : RED);
        g.drawString(mc.font, Component.translatable("hud.skofcraft.f14.fuel"), cx - 162, y, fuelColor, false);
        g.fill(cx - 162, y + 10, cx - 102, y + 13, 0xFF0A0C0A);
        g.fill(cx - 162, y + 10, cx - 162 + (int) (60 * fuel), y + 13, fuelColor);
        if (jet.getFuel() <= 0 && now / 4 % 2 == 0) {
            g.drawCenteredString(mc.font, Component.translatable("hud.skofcraft.f14.no_fuel"), cx, y - 14, RED);
        }
    }
}
