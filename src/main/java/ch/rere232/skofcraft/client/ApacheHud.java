package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.entity.ApacheHelicopter;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** Cockpit display shown while piloting: reticle, horizon, flight data, weapons, hull and fuel. */
public final class ApacheHud {
    private static final int GREEN = 0xFF55FF88;
    private static final int GREEN_DIM = 0xFF2E9E50;
    private static final int AMBER = 0xFFFFB347;
    private static final int RED = 0xFFFF5555;
    private static final String[] COMPASS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    private ApacheHud() {}

    public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (!(AircraftClient.piloted() instanceof ApacheHelicopter helicopter) || mc.options.hideGui) {
            return;
        }
        int cx = width / 2;
        int cy = height / 2;

        if (mc.options.getCameraType().isFirstPerson()) {
            drawCockpit(g, width, height);
        }
        drawHorizon(g, helicopter, cx, cy, partialTick);
        drawReticle(g, cx, cy);
        drawFlightData(g, helicopter, mc, cx, cy);
        drawWeapons(g, helicopter, mc, cx, height);
    }

    private static void drawCockpit(GuiGraphics g, int width, int height) {
        int dash = height / 7;
        g.fillGradient(0, height - dash, width, height, 0x00101410, 0xEE101410);
        g.fill(0, height - 3, width, height, 0xFF0A0C0A);
        g.fillGradient(0, 0, width, height / 12, 0xCC101410, 0x00101410);
        g.fillGradient(0, 0, width / 14, height, 0x99101410, 0x00101410);
        g.fillGradient(width - width / 14, 0, width, height, 0x00101410, 0x99101410);
    }

    private static void drawReticle(GuiGraphics g, int cx, int cy) {
        g.fill(cx - 14, cy, cx - 5, cy + 1, GREEN);
        g.fill(cx + 5, cy, cx + 14, cy + 1, GREEN);
        g.fill(cx, cy - 14, cx + 1, cy - 5, GREEN);
        g.fill(cx, cy + 5, cx + 1, cy + 14, GREEN);
        g.fill(cx, cy, cx + 1, cy + 1, GREEN);
        for (int i = 0; i < 4; i++) {
            int sx = (i % 2 == 0 ? -1 : 1) * 9;
            int sy = (i < 2 ? -1 : 1) * 9;
            g.fill(cx + sx, cy + sy, cx + sx + 1, cy + sy + 1, GREEN_DIM);
        }
    }

    private static void drawHorizon(GuiGraphics g, ApacheHelicopter helicopter, int cx, int cy, float partialTick) {
        float roll = Mth.lerp(partialTick, helicopter.tiltRollO, helicopter.tiltRoll);
        float pitch = Mth.lerp(partialTick, helicopter.tiltPitchO, helicopter.tiltPitch);
        g.pose().pushPose();
        g.pose().translate(cx, cy + pitch * 3.0F, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(roll));
        g.fill(-70, 0, -30, 1, GREEN_DIM);
        g.fill(30, 0, 70, 1, GREEN_DIM);
        g.fill(-70, 0, -69, 4, GREEN_DIM);
        g.fill(69, 0, 70, 4, GREEN_DIM);
        g.pose().popPose();
    }

    private static void drawFlightData(GuiGraphics g, ApacheHelicopter helicopter, Minecraft mc, int cx, int cy) {
        int speed = (int) Math.round(Math.hypot(helicopter.getX() - helicopter.xo, helicopter.getZ() - helicopter.zo) * 20.0D);
        int altitude = (int) Math.floor(helicopter.getY());
        int heading = Math.floorMod(Math.round(helicopter.getYRot() + 180.0F), 360);
        String cardinal = COMPASS[Math.floorMod(Math.round(heading / 45.0F), 8)];

        int left = cx - 110;
        g.drawString(mc.font, Component.translatable("hud.skofcraft.apache.spd", speed), left - 30, cy - 12, GREEN, false);
        g.drawString(mc.font, Component.translatable("hud.skofcraft.apache.alt", altitude), left - 30, cy, GREEN, false);
        g.drawString(mc.font, Component.translatable("hud.skofcraft.apache.hdg", heading, cardinal), cx + 70, cy - 6, GREEN, false);
    }

    private static void drawWeapons(GuiGraphics g, ApacheHelicopter helicopter, Minecraft mc, int cx, int height) {
        long now = mc.level != null ? mc.level.getGameTime() : 0L;
        float reload = Mth.clamp((now - AircraftClient.lastSecondaryTick) / (float) ApacheHelicopter.ROCKET_COOLDOWN, 0.0F, 1.0F);
        int y = height - 36;

        g.drawString(mc.font, Component.translatable("hud.skofcraft.apache.gun"), cx - 90, y, GREEN, false);
        g.fill(cx - 90, y + 10, cx - 30, y + 13, GREEN);

        g.drawString(mc.font, Component.translatable("hud.skofcraft.apache.rockets"), cx - 18, y, reload < 1.0F ? AMBER : GREEN, false);
        g.fill(cx - 18, y + 10, cx + 42, y + 13, 0xFF0A0C0A);
        g.fill(cx - 18, y + 10, cx - 18 + (int) (60 * reload), y + 13, reload < 1.0F ? AMBER : GREEN);

        float hull = Mth.clamp(helicopter.getHealth() / ApacheHelicopter.MAX_HEALTH, 0.0F, 1.0F);
        int hullColor = hull > 0.5F ? GREEN : (hull > 0.25F ? AMBER : RED);
        g.drawString(mc.font, Component.translatable("hud.skofcraft.apache.hull"), cx + 54, y, hullColor, false);
        g.fill(cx + 54, y + 10, cx + 114, y + 13, 0xFF0A0C0A);
        g.fill(cx + 54, y + 10, cx + 54 + (int) (60 * hull), y + 13, hullColor);

        float fuel = Mth.clamp(helicopter.getFuel() / (float) helicopter.getMaxFuel(), 0.0F, 1.0F);
        int fuelColor = fuel > 0.25F ? GREEN : (fuel > 0.1F ? AMBER : RED);
        g.drawString(mc.font, Component.translatable("hud.skofcraft.apache.fuel"), cx - 162, y, fuelColor, false);
        g.fill(cx - 162, y + 10, cx - 102, y + 13, 0xFF0A0C0A);
        g.fill(cx - 162, y + 10, cx - 162 + (int) (60 * fuel), y + 13, fuelColor);
        if (helicopter.getFuel() <= 0 && now / 4 % 2 == 0) {
            g.drawCenteredString(mc.font, Component.translatable("hud.skofcraft.apache.no_fuel"), cx, y - 14, RED);
        }
    }
}
