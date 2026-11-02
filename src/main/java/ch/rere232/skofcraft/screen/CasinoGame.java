package ch.rere232.skofcraft.screen;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.casino.SlotMachine;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** One casino table below the shared header: draws itself and reacts to its own buttons. */
@OnlyIn(Dist.CLIENT)
abstract class CasinoGame {
    protected static final int GOLD = 0xFFF2C14E;
    protected static final int RED = 0xFFC8283A;
    protected static final int DIM = 0xFFB98A8F;
    private static final int BET_BUTTON_BASE = 100;
    private static final int BET_W = 30;
    private static final int BET_H = 16;

    protected final CasinoApp host;
    protected final Font font;
    /** Registered this frame: id, x, y, w, h, enabled(1/0). */
    private final List<int[]> buttons = new ArrayList<>();

    protected CasinoGame(CasinoApp host, Font font) {
        this.host = host;
        this.font = font;
    }

    abstract void render(GuiGraphics g, int x, int y, int w, int h, float dt);

    protected abstract void onButton(int id);

    /** A busy table (waiting on the server or animating) blocks switching tabs. */
    boolean isBusy() {
        return false;
    }

    void onShow() {}

    boolean keyPressed(int keyCode) {
        return false;
    }

    final void beginFrame() {
        buttons.clear();
    }

    final boolean mouseClicked(double mouseX, double mouseY) {
        for (int[] b : buttons) {
            if (b[5] == 1 && mouseX >= b[1] && mouseX < b[1] + b[3] && mouseY >= b[2] && mouseY < b[2] + b[4]) {
                if (b[0] >= BET_BUTTON_BASE && b[0] < BET_BUTTON_BASE + SlotMachine.BETS.length) {
                    CasinoApp.selectedBet = b[0] - BET_BUTTON_BASE;
                    host.play(SkofcraftSounds.UI_CLICK.get(), 1.2F);
                } else {
                    onButton(b[0]);
                }
                return true;
            }
        }
        return false;
    }

    protected void button(GuiGraphics g, int id, int x, int y, int w, int h, Component label, int bg, int fg,
                          boolean enabled, boolean selected) {
        if (selected) {
            MachineScreenHelper.fillRounded(g, x - 1, y - 1, x + w + 1, y + h + 1, GOLD);
        }
        MachineScreenHelper.fillRounded(g, x, y, x + w, y + h, enabled ? bg : 0xFF2A2F38);
        float scale = 0.85F;
        g.pose().pushPose();
        g.pose().translate(x + (w - font.width(label) * scale) / 2.0F, y + (h - 8 * scale) / 2.0F + 0.5F, 0);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(font, label, 0, 0, enabled ? fg : MachineScreenHelper.COLOR_TEXT_DIM, false);
        g.pose().popPose();
        buttons.add(new int[] {id, x, y, w, h, enabled ? 1 : 0});
    }

    /** The shared row of 1/2/5 diamond bet buttons. */
    protected void betRow(GuiGraphics g, int x, int y, int w, boolean enabled) {
        int gap = 4;
        int total = SlotMachine.BETS.length * BET_W + (SlotMachine.BETS.length - 1) * gap;
        int bx = x + (w - total) / 2;
        for (int i = 0; i < SlotMachine.BETS.length; i++) {
            int px = bx + i * (BET_W + gap);
            boolean selected = i == CasinoApp.selectedBet;
            MachineScreenHelper.fillRounded(g, px, y, px + BET_W, y + BET_H, selected ? GOLD : 0xFF2A1A1E);
            g.pose().pushPose();
            g.pose().translate(px + 4, y + 4, 0);
            g.pose().scale(0.5F, 0.5F, 1.0F);
            g.renderItem(new ItemStack(Items.DIAMOND), 0, 0);
            g.pose().popPose();
            g.pose().pushPose();
            g.pose().translate(px + 15, y + 4, 0);
            g.pose().scale(0.9F, 0.9F, 1.0F);
            g.drawString(font, Integer.toString(SlotMachine.BETS[i]), 0, 0, selected ? 0xFF111111 : 0xFFD5D8DD, false);
            g.pose().popPose();
            buttons.add(new int[] {BET_BUTTON_BASE + i, px, y, BET_W, BET_H, enabled ? 1 : 0});
        }
    }

    protected void drawSmall(GuiGraphics g, String text, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.7F, 0.7F, 1.0F);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    protected void drawCentered(GuiGraphics g, Component text, int cx, int y, float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(cx - font.width(text) * scale / 2.0F, y, 0);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }
}
