package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.entity.AircraftFuel;
import ch.rere232.skofcraft.entity.Tank;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/**
 * The gunner's sight, kept to the edges so the view stays clear. A small reticle marks where you're looking and a
 * square marks where the barrel really points, so you can watch it catch up. Top right, a plan view shows which way the
 * hull and turret face; bottom left, the tank's state; above the hotbar, the main gun's reload.
 */
public final class TankHud {
    private static final int GREEN = 0xFF55FF88;
    private static final int GREEN_DIM = 0xAA2E9E50;
    private static final int AMBER = 0xFFFFB347;
    private static final int RED = 0xFFFF5555;
    private static final int PANEL = 0x66000000;
    private static final int KMH_AT_ONE_BLOCK_PER_TICK = 72;
    private static final float ALIGNED_DEGREES = 2.0F;
    private static final double RANGE_LIMIT = 256.0D;
    private static final int RELOAD_BAR_WIDTH = 90;
    private static final int STATUS_BAR_WIDTH = 50;
    private static final int PLAN_SIZE = 44;
    private static final int MARGIN = 6;
    private static final int BRACKET = 22;

    private TankHud() {}

    public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (!(AircraftClient.piloted() instanceof Tank tank) || mc.options.hideGui || mc.player == null || mc.level == null) {
            return;
        }
        int cx = width / 2;
        int cy = height / 2;
        if (mc.options.getCameraType().isFirstPerson()) {
            drawSightFrame(g, width, height);
        }
        drawAim(g, mc, tank, cx, cy, height, partialTick);
        drawRange(g, mc, cx, cy);
        drawStatus(g, mc, tank, height);
        drawPlan(g, mc, tank, width, partialTick);
        drawReload(g, mc, tank, cx, height);
    }

    /** Just the corners: enough to feel like looking through a sight without covering anything. */
    private static void drawSightFrame(GuiGraphics g, int width, int height) {
        int m = MARGIN;
        g.fill(m, m, m + BRACKET, m + 1, GREEN_DIM);
        g.fill(m, m, m + 1, m + BRACKET, GREEN_DIM);
        g.fill(width - m - BRACKET, m, width - m, m + 1, GREEN_DIM);
        g.fill(width - m - 1, m, width - m, m + BRACKET, GREEN_DIM);
        g.fill(m, height - m - 1, m + BRACKET, height - m, GREEN_DIM);
        g.fill(m, height - m - BRACKET, m + 1, height - m, GREEN_DIM);
        g.fill(width - m - BRACKET, height - m - 1, width - m, height - m, GREEN_DIM);
        g.fill(width - m - 1, height - m - BRACKET, width - m, height - m, GREEN_DIM);
    }

    private static void drawAim(GuiGraphics g, Minecraft mc, Tank tank, int cx, int cy, int height, float partialTick) {
        float yawError = Mth.wrapDegrees(Mth.rotLerp(partialTick, tank.turretYawO, tank.turretYaw) - mc.player.getViewYRot(partialTick));
        float pitchError = Mth.lerp(partialTick, tank.gunPitchO, tank.gunPitch) - mc.player.getViewXRot(partialTick);
        boolean aligned = Math.abs(yawError) <= ALIGNED_DEGREES && Math.abs(pitchError) <= ALIGNED_DEGREES;
        int color = aligned ? GREEN : AMBER;

        // A small crosshair where you're looking
        g.fill(cx - 9, cy, cx - 3, cy + 1, color);
        g.fill(cx + 4, cy, cx + 10, cy + 1, color);
        g.fill(cx, cy - 9, cx + 1, cy - 3, color);
        g.fill(cx, cy + 4, cx + 1, cy + 10, color);

        // A square where the barrel is pointing, held to the screen's edge if it's behind you
        double pixelsPerDegree = height / mc.options.fov().get().doubleValue();
        int marginX = cx - 12;
        int marginY = cy - 12;
        int bx = cx + Mth.clamp((int) (yawError * pixelsPerDegree), -marginX, marginX);
        int by = cy + Mth.clamp((int) (pitchError * pixelsPerDegree), -marginY, marginY);
        g.fill(bx - 3, by - 3, bx + 4, by - 2, color);
        g.fill(bx - 3, by + 3, bx + 4, by + 4, color);
        g.fill(bx - 3, by - 3, bx - 2, by + 4, color);
        g.fill(bx + 3, by - 3, bx + 4, by + 4, color);
    }

    private static void drawRange(GuiGraphics g, Minecraft mc, int cx, int cy) {
        Vec3 eye = mc.player.getEyePosition();
        Vec3 end = eye.add(mc.player.getLookAngle().scale(RANGE_LIMIT));
        HitResult hit = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        String range = hit.getType() == HitResult.Type.MISS ? "---" : Integer.toString((int) hit.getLocation().distanceTo(eye));
        g.drawString(mc.font, Component.translatable("hud.skofcraft.maus.range", range), cx + 14, cy + 6, GREEN, true);
    }

    private static void drawStatus(GuiGraphics g, Minecraft mc, Tank tank, int height) {
        int x = MARGIN + 6;
        int y = height - MARGIN - 40;
        g.fill(x - 4, y - 4, x + STATUS_BAR_WIDTH + 34, y + 36, PANEL);
        int kmh = Math.round(tank.shownSpeed * KMH_AT_ONE_BLOCK_PER_TICK);
        g.drawString(mc.font, kmh + " km/h", x, y, GREEN, true);

        float hull = tank.getHealth() / tank.getMaxHealth();
        drawBar(g, mc, Component.translatable("hud.skofcraft.apache.hull"), x, y + 11, hull, hull < 0.25F ? RED : GREEN);
        if (tank.getFuel() > 0) {
            float fuel = AircraftFuel.percent(tank) / 100.0F;
            drawBar(g, mc, Component.translatable("hud.skofcraft.apache.fuel"), x, y + 22, fuel, fuel < 0.1F ? AMBER : GREEN);
        } else {
            g.drawString(mc.font, Component.translatable("hud.skofcraft.apache.no_fuel"), x, y + 22, RED, true);
        }
    }

    private static void drawBar(GuiGraphics g, Minecraft mc, Component label, int x, int y, float fraction, int color) {
        g.drawString(mc.font, label, x, y, color, true);
        int barX = x + 30;
        g.fill(barX, y + 2, barX + STATUS_BAR_WIDTH, y + 6, PANEL);
        g.fill(barX, y + 2, barX + (int) (STATUS_BAR_WIDTH * Mth.clamp(fraction, 0.0F, 1.0F)), y + 6, color);
    }

    /** Seen from above with your view pointing up: the hull turns under you, and the line is the barrel. */
    private static void drawPlan(GuiGraphics g, Minecraft mc, Tank tank, int width, float partialTick) {
        int size = PLAN_SIZE;
        int centerX = width - MARGIN - 8 - size / 2;
        int centerY = MARGIN + 8 + size / 2;
        g.fill(centerX - size / 2, centerY - size / 2, centerX + size / 2, centerY + size / 2, PANEL);
        float view = mc.player.getViewYRot(partialTick);

        g.pose().pushPose();
        g.pose().translate(centerX, centerY, 0.0F);
        g.pose().pushPose();
        g.pose().mulPose(Axis.ZP.rotationDegrees(tank.getViewYRot(partialTick) - view));
        g.fill(-7, -14, 7, 14, GREEN_DIM);
        g.fill(-5, -14, 5, -12, GREEN);
        g.pose().popPose();
        g.pose().mulPose(Axis.ZP.rotationDegrees(Mth.rotLerp(partialTick, tank.turretYawO, tank.turretYaw) - view));
        g.fill(-4, -4, 5, 5, GREEN);
        g.fill(0, -18, 1, -4, GREEN);
        g.pose().popPose();
    }

    private static void drawReload(GuiGraphics g, Minecraft mc, Tank tank, int cx, int height) {
        long sinceShot = mc.level.getGameTime() - AircraftClient.lastSecondaryTick;
        float reload = Mth.clamp((float) sinceShot / tank.secondaryCooldownTicks(), 0.0F, 1.0F);
        boolean ready = reload >= 1.0F;
        int color = ready ? GREEN : AMBER;
        int barX = cx - RELOAD_BAR_WIDTH / 2;
        int barY = height - 58;
        g.drawCenteredString(mc.font, Component.translatable(ready ? "hud.skofcraft.maus.ready" : "hud.skofcraft.maus.reloading"), cx, barY - 10, color);
        g.fill(barX, barY, barX + RELOAD_BAR_WIDTH, barY + 2, PANEL);
        g.fill(barX, barY, barX + (int) (RELOAD_BAR_WIDTH * reload), barY + 2, color);
    }
}
