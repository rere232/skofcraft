package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.entity.FpvDrone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** An analog video feed with an on-screen display burned into it, and the static when either of those goes wrong. */
public final class FpvDroneHud {
    private static final int WHITE = 0xFFF2F2F2;
    private static final int AMBER = 0xFFFFB347;
    private static final int RED = 0xFFFF4040;
    /** Where the picture starts breaking up, as a share of the signal range. */
    private static final float BREAKUP_FROM = 0.65F;
    /** A full pack's resting voltage and the point it is flat, for a 4S lipo. */
    private static final float VOLTS_FULL = 16.8F;
    private static final float VOLTS_EMPTY = 13.2F;
    private static final double METRES_PER_SECOND_TO_KMH = 3.6D;

    private static final RandomSource NOISE = RandomSource.create();

    private FpvDroneHud() {}

    public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        FpvDrone drone = FpvDroneClient.linked();
        if (drone == null) {
            if (FpvDroneClient.noSignalTicks() > 0) {
                drawNoSignal(g, mc, width, height);
            }
            return;
        }
        float signalUsed = FpvDroneClient.signalUsed(drone);
        float breakup = Mth.clamp((signalUsed - BREAKUP_FROM) / (1.0F - BREAKUP_FROM), 0.0F, 1.0F);
        drawStatic(g, width, height, 0.015F + breakup * breakup * 0.6F);
        if (mc.options.hideGui) {
            return;
        }
        drawReticle(g, width / 2, height / 2);
        drawTelemetry(g, mc, drone, signalUsed, width, height);
    }

    private static void drawReticle(GuiGraphics g, int cx, int cy) {
        g.fill(cx - 8, cy, cx - 3, cy + 1, WHITE);
        g.fill(cx + 4, cy, cx + 9, cy + 1, WHITE);
        g.fill(cx, cy + 4, cx + 1, cy + 8, WHITE);
        g.fill(cx, cy, cx + 1, cy + 1, WHITE);
    }

    private static void drawTelemetry(GuiGraphics g, Minecraft mc, FpvDrone drone, float signalUsed, int width, int height) {
        int left = 12;
        int right = width - 12;
        int top = 10;
        int bottom = height - 18;

        float battery = drone.getBattery() / (float) FpvDrone.MAX_BATTERY;
        float volts = Mth.lerp(battery, VOLTS_EMPTY, VOLTS_FULL);
        int batteryColor = battery > 0.25F ? WHITE : battery > 0.1F ? AMBER : RED;
        g.drawString(mc.font, Component.translatable("hud.skofcraft.fpv.battery", String.format(java.util.Locale.ROOT, "%.1f", volts)), left, top, batteryColor, true);

        int flightSeconds = drone.getFlightTicks() / 20;
        g.drawString(mc.font, String.format(java.util.Locale.ROOT, "%d:%02d", flightSeconds / 60, flightSeconds % 60), left, top + 11, WHITE, true);

        int signal = Mth.clamp(Math.round((1.0F - signalUsed) * 100.0F), 0, 100);
        int signalColor = signal > 35 ? WHITE : signal > 15 ? AMBER : RED;
        Component signalText = Component.translatable("hud.skofcraft.fpv.signal", signal);
        g.drawString(mc.font, signalText, right - mc.font.width(signalText), top, signalColor, true);

        Component homeText = Component.translatable("hud.skofcraft.fpv.home", mc.player == null ? 0 : Math.round(drone.horizontalDistanceTo(mc.player)));
        g.drawString(mc.font, homeText, right - mc.font.width(homeText), top + 11, WHITE, true);

        double metresPerSecond = drone.position().subtract(drone.xo, drone.yo, drone.zo).length() * 20.0D;
        g.drawString(mc.font, Component.translatable("hud.skofcraft.fpv.speed", Math.round(metresPerSecond * METRES_PER_SECOND_TO_KMH)), left, bottom, WHITE, true);
        Component altitude = Component.translatable("hud.skofcraft.fpv.altitude", (int) Math.floor(drone.getY()));
        g.drawString(mc.font, altitude, right - mc.font.width(altitude), bottom, WHITE, true);

        boolean blink = mc.level != null && mc.level.getGameTime() / 4 % 2 == 0;
        if (drone.isArmed()) {
            g.drawCenteredString(mc.font, Component.translatable("hud.skofcraft.fpv.armed"), width / 2, bottom, RED);
        } else {
            g.drawCenteredString(mc.font, Component.translatable("hud.skofcraft.fpv.arming"), width / 2, bottom, AMBER);
        }
        if (drone.getBattery() <= 0 && blink) {
            g.drawCenteredString(mc.font, Component.translatable("hud.skofcraft.fpv.flat_battery"), width / 2, height / 3, RED);
        }
        if (signalUsed > 0.85F && blink) {
            g.drawCenteredString(mc.font, Component.translatable("hud.skofcraft.fpv.weak_signal"), width / 2, height / 3 + 11, AMBER);
        }
        // Nothing else tells you, with the normal health bar gone along with your eyes
        if (mc.player != null && mc.player.hurtTime > 0) {
            g.fill(0, 0, width, height, 0x30FF0000);
            g.drawCenteredString(mc.font, Component.translatable("hud.skofcraft.fpv.pilot_hit", (int) Math.ceil(mc.player.getHealth())), width / 2, height / 3 - 11, RED);
        }
    }

    private static void drawNoSignal(GuiGraphics g, Minecraft mc, int width, int height) {
        g.fill(0, 0, width, height, 0xFF141414);
        drawStatic(g, width, height, 1.0F);
        g.drawCenteredString(mc.font, Component.translatable("hud.skofcraft.fpv.no_signal"), width / 2, height / 2 - 4, WHITE);
    }

    /** Random bright streaks, the way a weak analog signal tears up. */
    private static void drawStatic(GuiGraphics g, int width, int height, float amount) {
        int streaks = (int) (height * 1.5F * amount);
        for (int i = 0; i < streaks; i++) {
            int y = NOISE.nextInt(height);
            int length = 4 + NOISE.nextInt(1 + (int) (width * (0.05F + amount * 0.5F)));
            int x = NOISE.nextInt(width);
            int alpha = 30 + NOISE.nextInt(120);
            g.fill(x, y, x + length, y + 1, alpha << 24 | 0xDDDDDD);
        }
    }
}
