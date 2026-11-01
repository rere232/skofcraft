package ch.rere232.skofcraft.screen;

import java.util.function.Function;
import ch.rere232.skofcraft.client.PhoneClient;
import ch.rere232.skofcraft.network.PhoneUseC2SPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** The phone shell: body, status bar, battery and a home screen that launches apps. */
@OnlyIn(Dist.CLIENT)
public class PhoneScreen extends Screen {
    public static final int PHONE_W = 148;
    public static final int BEZEL = 6;
    public static final int SCREEN_W = PHONE_W - BEZEL * 2;
    private static final int MAX_PHONE_H = 224;
    private static final int STATUS_H = 12;
    private static final int HOME_BAR_H = 8;
    private static final int ICON_SIZE = 28;
    private static final int ICON_COLUMN_W = SCREEN_W / 3;
    private static final int HEARTBEAT_TICKS = 60;

    private enum AppIcon {
        SCROLLR("phone.skofcraft.app", ScrollrApp::new),
        AMAZOOON("phone.skofcraft.amazooon", AmazooonApp::new),
        FLAPPY("phone.skofcraft.flappy", FlappyApp::new),
        CASINO("phone.skofcraft.casino", CasinoApp::new),
        MAP("phone.skofcraft.map", MapApp::new);

        final String nameKey;
        final Function<Font, PhoneApp> factory;

        AppIcon(String nameKey, Function<Font, PhoneApp> factory) {
            this.nameKey = nameKey;
            this.factory = factory;
        }
    }

    /** Battery persists between openings and recharges while the phone is closed. */
    private static float battery = 100.0F;
    private static long lastClosedMs = System.currentTimeMillis();

    private PhoneApp app;
    private int appId;
    private int useTicks;
    private int phoneX;
    private int phoneY;
    private int phoneH;

    public PhoneScreen() {
        super(Component.translatable("item.skofcraft.phone"));
    }

    @Override
    protected void init() {
        super.init();
        phoneH = Math.min(MAX_PHONE_H, this.height - 8);
        phoneX = (this.width - PHONE_W) / 2;
        phoneY = (this.height - phoneH) / 2;

        long now = System.currentTimeMillis();
        battery = Math.min(100.0F, battery + (now - lastClosedMs) / 1000.0F * 0.5F);
    }

    @Override
    public void onClose() {
        closeApp();
        lastClosedMs = System.currentTimeMillis();
        super.onClose();
    }

    private void closeApp() {
        if (app != null) {
            app.onClose();
            app = null;
            appId = 0;
        }
    }

    private boolean isDead() {
        return battery <= 0.0F;
    }

    private int screenX() {
        return phoneX + BEZEL;
    }

    private int screenY() {
        return phoneY + BEZEL;
    }

    private int screenH() {
        return phoneH - BEZEL * 2;
    }

    private int appY() {
        return screenY() + STATUS_H;
    }

    private int appH() {
        return screenH() - STATUS_H - HOME_BAR_H;
    }

    @Override
    public void tick() {
        if (isDead()) {
            return;
        }
        battery = Math.max(0.0F, battery - 100.0F / (20.0F * 150.0F));
        if (useTicks++ % HEARTBEAT_TICKS == 0) {
            SkofcraftNetwork.CHANNEL.sendToServer(new PhoneUseC2SPacket(appId));
        }
        if (app != null) {
            app.tick();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        drawBody(g);

        int sx = screenX();
        int sy = screenY();
        int sw = SCREEN_W;
        int sh = screenH();
        g.fill(sx, sy, sx + sw, sy + sh, 0xFF101318);

        if (isDead()) {
            drawDeadScreen(g, sx, sy, sw, sh);
        } else if (app == null) {
            drawHome(g, sx, sy, sw, sh);
        } else {
            g.enableScissor(sx, appY(), sx + sw, appY() + appH());
            app.render(g, this.font, sx, appY(), sw, appH(), mouseX, mouseY);
            g.disableScissor();
        }
        drawStatusBar(g, sx, sy, sw);
        g.fill(sx + sw / 2 - 18, sy + sh - 5, sx + sw / 2 + 18, sy + sh - 3, app == null ? 0xFF5A606B : 0xFFB8BDC7);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawBody(GuiGraphics g) {
        MachineScreenHelper.fillRounded(g, phoneX + 3, phoneY + 3, phoneX + PHONE_W + 3, phoneY + phoneH + 3, 0x66000000);
        MachineScreenHelper.fillRounded(g, phoneX, phoneY, phoneX + PHONE_W, phoneY + phoneH, 0xFF05060A);
        MachineScreenHelper.fillRounded(g, phoneX + 1, phoneY + 1, phoneX + PHONE_W - 1, phoneY + phoneH - 1, 0xFF1B1E25);
        g.fill(phoneX + 3, phoneY + 3, phoneX + PHONE_W - 3, phoneY + phoneH - 3, 0xFF05060A);
    }

    private void drawDeadScreen(GuiGraphics g, int sx, int sy, int sw, int sh) {
        g.fill(sx, sy, sx + sw, sy + sh, 0xFF000000);
        int cx = sx + sw / 2;
        int cy = sy + sh / 2 - 10;
        g.fill(cx - 10, cy, cx + 10, cy + 12, 0xFF6E7480);
        g.fill(cx - 9, cy + 1, cx + 9, cy + 11, 0xFF000000);
        g.fill(cx + 10, cy + 4, cx + 12, cy + 8, 0xFF6E7480);
        g.fill(cx - 8, cy + 2, cx - 5, cy + 10, 0xFFE5484D);
        Component text = Component.translatable("phone.skofcraft.dead");
        g.drawCenteredString(this.font, text, cx, cy + 20, 0xFF9AA0A6);
    }

    private static String clock() {
        int ticks = 0;
        if (Minecraft.getInstance().level != null) {
            ticks = (int) (Minecraft.getInstance().level.getDayTime() % 24000L);
        }
        int hours = (ticks / 1000 + 6) % 24;
        int minutes = (ticks % 1000) * 60 / 1000;
        return String.format("%02d:%02d", hours, minutes);
    }

    private void drawStatusBar(GuiGraphics g, int sx, int sy, int sw) {
        g.pose().pushPose();
        g.pose().translate(sx + 6, sy + 3, 0);
        g.pose().scale(0.8F, 0.8F, 1.0F);
        g.drawString(this.font, clock(), 0, 0, MachineScreenHelper.COLOR_TEXT, false);
        g.pose().popPose();

        int bx = sx + sw - 24;
        int fill = Math.round(14 * battery / 100.0F);
        int color = battery > 50 ? 0xFF3DDC97 : (battery > 20 ? 0xFFE0A030 : 0xFFE5484D);
        g.fill(bx, sy + 3, bx + 16, sy + 9, 0xFF6E7480);
        g.fill(bx + 1, sy + 4, bx + 15, sy + 8, 0xFF05060A);
        g.fill(bx + 1, sy + 4, bx + 1 + fill, sy + 8, color);
        g.fill(bx + 16, sy + 5, bx + 17, sy + 7, 0xFF6E7480);
        for (int i = 0; i < 4; i++) {
            g.fill(sx + sw - 44 + i * 3, sy + 8 - i * 2, sx + sw - 42 + i * 3, sy + 9, 0xFFB8BDC7);
        }
        g.fill(sx + sw / 2 - 14, sy, sx + sw / 2 + 14, sy + 4, 0xFF05060A);
    }

    private int iconX(int index) {
        return screenX() + (index % 3) * ICON_COLUMN_W + (ICON_COLUMN_W - ICON_SIZE) / 2;
    }

    private int iconY(int index) {
        return appY() + 44 + (index / 3) * 44;
    }

    private void drawHome(GuiGraphics g, int sx, int sy, int sw, int sh) {
        g.fillGradient(sx, sy, sx + sw, sy + sh, 0xFF1B2A4E, 0xFF4A2A63);
        String time = clock();
        g.pose().pushPose();
        g.pose().translate(sx + (sw - this.font.width(time) * 2.2F) / 2.0F, appY() + 10, 0);
        g.pose().scale(2.2F, 2.2F, 1.0F);
        g.drawString(this.font, time, 0, 0, 0xFFFFFFFF, false);
        g.pose().popPose();

        AppIcon[] icons = AppIcon.values();
        for (int i = 0; i < icons.length; i++) {
            int x = iconX(i);
            int y = iconY(i);
            drawIcon(g, icons[i], x, y);
            drawBadge(g, i, x + ICON_SIZE - 6, y - 4);
            Component label = Component.translatable(icons[i].nameKey);
            g.pose().pushPose();
            g.pose().translate(x + (ICON_SIZE - this.font.width(label) * 0.7F) / 2.0F, y + ICON_SIZE + 3, 0);
            g.pose().scale(0.7F, 0.7F, 1.0F);
            g.drawString(this.font, label, 0, 0, 0xFFFFFFFF, true);
            g.pose().popPose();
        }
        drawWellbeing(g, sx + 6, appY() + appH() - 32, sw - 12);
    }

    /** A deeper habit makes more apps show a red notification count. */
    private void drawBadge(GuiGraphics g, int appIndex, int x, int y) {
        int level = PhoneClient.addiction();
        if (level < 25 + appIndex * 15) {
            return;
        }
        String count = Integer.toString(Math.min(99, 1 + level * (appIndex + 1) / 6));
        int w = Math.max(9, (int) (this.font.width(count) * 0.7F) + 5);
        MachineScreenHelper.fillRounded(g, x, y, x + w, y + 9, 0xFFFFFFFF);
        MachineScreenHelper.fillRounded(g, x + 1, y + 1, x + w - 1, y + 8, 0xFFE5484D);
        g.pose().pushPose();
        g.pose().translate(x + (w - this.font.width(count) * 0.7F) / 2.0F, y + 2, 0);
        g.pose().scale(0.7F, 0.7F, 1.0F);
        g.drawString(this.font, count, 0, 0, 0xFFFFFFFF, false);
        g.pose().popPose();
    }

    private void drawWellbeing(GuiGraphics g, int x, int y, int w) {
        int level = Math.max(0, Math.min(100, PhoneClient.addiction()));
        MachineScreenHelper.fillRounded(g, x, y, x + w, y + 30, 0x88000000);

        g.pose().pushPose();
        g.pose().translate(x + 4, y + 3, 0);
        g.pose().scale(0.7F, 0.7F, 1.0F);
        g.drawString(this.font, Component.translatable("phone.skofcraft.wellbeing"), 0, 0, 0xFFFFFFFF, false);
        g.pose().popPose();
        String percent = level + "%";
        g.pose().pushPose();
        g.pose().translate(x + w - 4 - this.font.width(percent) * 0.7F, y + 3, 0);
        g.pose().scale(0.7F, 0.7F, 1.0F);
        g.drawString(this.font, percent, 0, 0, MachineScreenHelper.COLOR_TEXT_DIM, false);
        g.pose().popPose();

        int color = level > 70 ? 0xFFE5484D : (level > 35 ? 0xFFEE7B30 : 0xFF3DDC97);
        g.fill(x + 4, y + 13, x + w - 4, y + 18, 0xFF05060A);
        g.fill(x + 5, y + 14, x + 5 + (w - 10) * level / 100, y + 17, color);

        int tier = level < 25 ? 0 : (level < 50 ? 1 : (level < 75 ? 2 : 3));
        g.pose().pushPose();
        g.pose().translate(x + 4, y + 21, 0);
        g.pose().scale(0.65F, 0.65F, 1.0F);
        g.drawString(this.font, Component.translatable("phone.skofcraft.wellbeing." + tier), 0, 0, 0xFFD5D8DD, false);
        g.pose().popPose();
    }

    private void drawIcon(GuiGraphics g, AppIcon icon, int x, int y) {
        MachineScreenHelper.fillRounded(g, x - 1, y - 1, x + ICON_SIZE + 1, y + ICON_SIZE + 1, 0x66000000);
        if (icon == AppIcon.SCROLLR) {
            MachineScreenHelper.fillRounded(g, x, y, x + ICON_SIZE, y + ICON_SIZE, 0xFF8B3FE4);
            g.fillGradient(x + 1, y + 1, x + ICON_SIZE - 1, y + ICON_SIZE - 1, 0xFFB44FE0, 0xFFE5484D);
            g.pose().pushPose();
            g.pose().translate(x + 8, y + 7, 0);
            g.pose().scale(2.0F, 2.0F, 1.0F);
            g.drawString(this.font, Component.literal("S").withStyle(ChatFormatting.BOLD), 0, 0, 0xFFFFFFFF, false);
            g.pose().popPose();
        } else if (icon == AppIcon.FLAPPY) {
            MachineScreenHelper.fillRounded(g, x, y, x + ICON_SIZE, y + ICON_SIZE, 0xFF4EC0CA);
            g.fill(x + 20, y + 14, x + 26, y + ICON_SIZE - 1, 0xFF73BF2E);
            g.fill(x + 19, y + 14, x + 27, y + 17, 0xFF4B8A1C);
            g.fill(x + 1, y + ICON_SIZE - 5, x + ICON_SIZE - 1, y + ICON_SIZE - 1, 0xFFDED895);
            g.fill(x + 6, y + 8, x + 16, y + 16, 0xFF533C1C);
            g.fill(x + 7, y + 9, x + 15, y + 15, 0xFFF8D030);
            g.fill(x + 12, y + 9, x + 15, y + 12, 0xFFFFFFFF);
            g.fill(x + 14, y + 10, x + 15, y + 11, 0xFF000000);
            g.fill(x + 14, y + 12, x + 18, y + 15, 0xFFE0502C);
            g.fill(x + 7, y + 11, x + 11, y + 14, 0xFFFFFFFF);
        } else if (icon == AppIcon.MAP) {
            // Satellite imagery: sea, land, a road and a red pin
            MachineScreenHelper.fillRounded(g, x, y, x + ICON_SIZE, y + ICON_SIZE, 0xFF2F6FB0);
            g.fill(x + 1, y + 9, x + 18, y + ICON_SIZE - 1, 0xFF5E9C3A);
            g.fill(x + 18, y + 14, x + 26, y + ICON_SIZE - 3, 0xFF7DB04A);
            g.fill(x + 4, y + 18, x + 24, y + 20, 0xFFD8C38A);
            g.fill(x + 12, y + 4, x + 18, y + 10, 0xFF7A1521);
            g.fill(x + 13, y + 5, x + 17, y + 9, 0xFFE5484D);
            g.fill(x + 14, y + 10, x + 16, y + 14, 0xFFE5484D);
            g.fill(x + 3, y + 3, x + 7, y + 5, 0xFFC8A030);
            g.fill(x + 1, y + 3, x + 3, y + 5, 0xFF9CC4F0);
            g.fill(x + 7, y + 3, x + 9, y + 5, 0xFF9CC4F0);
        } else if (icon == AppIcon.CASINO) {
            MachineScreenHelper.fillRounded(g, x, y, x + ICON_SIZE, y + ICON_SIZE, 0xFFF2C14E);
            MachineScreenHelper.fillRounded(g, x + 2, y + 2, x + ICON_SIZE - 2, y + ICON_SIZE - 2, 0xFF7A1521);
            g.pose().pushPose();
            g.pose().translate(x + 9, y + 6, 0);
            g.pose().scale(2.0F, 2.0F, 1.0F);
            g.drawString(this.font, Component.literal("7").withStyle(ChatFormatting.BOLD), 0, 0, 0xFFF2C14E, false);
            g.pose().popPose();
            g.fill(x + 6, y + 21, x + 11, y + 24, 0xFFE5484D);
            g.fill(x + 12, y + 21, x + 17, y + 24, 0xFFF2C14E);
            g.fill(x + 18, y + 21, x + 23, y + 24, 0xFF4EC0CA);
        } else {
            MachineScreenHelper.fillRounded(g, x, y, x + ICON_SIZE, y + ICON_SIZE, 0xFF131A22);
            g.pose().pushPose();
            g.pose().translate(x + 9, y + 4, 0);
            g.pose().scale(2.0F, 2.0F, 1.0F);
            g.drawString(this.font, Component.literal("a").withStyle(ChatFormatting.BOLD), 0, 0, 0xFFFFFFFF, false);
            g.pose().popPose();
            g.fill(x + 6, y + 21, x + 22, y + 22, 0xFFFF9900);
            g.fill(x + 4, y + 20, x + 6, y + 21, 0xFFFF9900);
            g.fill(x + 22, y + 20, x + 24, y + 21, 0xFFFF9900);
            g.fill(x + 23, y + 19, x + 25, y + 20, 0xFFFF9900);
        }
    }

    private boolean overHomeBar(double mouseX, double mouseY) {
        int top = screenY() + screenH() - HOME_BAR_H;
        return mouseX >= screenX() && mouseX < screenX() + SCREEN_W && mouseY >= top && mouseY < top + HOME_BAR_H;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isDead()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (overHomeBar(mouseX, mouseY)) {
            closeApp();
            return true;
        }
        if (app == null) {
            if (button == 0) {
                AppIcon[] icons = AppIcon.values();
                for (int i = 0; i < icons.length; i++) {
                    int x = iconX(i);
                    int y = iconY(i);
                    if (mouseX >= x && mouseX < x + ICON_SIZE && mouseY >= y && mouseY < y + ICON_SIZE) {
                        app = icons[i].factory.apply(this.font);
                        appId = i + 1;
                        return true;
                    }
                }
            }
        } else if (mouseY >= appY() && mouseY < appY() + appH() && app.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDead() || app == null) {
            return false;
        }
        return app.mouseDragged(dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (app != null) {
            app.mouseReleased();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isDead() && app != null) {
            app.mouseScrolled(delta);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        if (keyCode == 256 && app != null) {
            closeApp();
            return true;
        }
        if (app != null && !isDead() && app.keyPressed(keyCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
