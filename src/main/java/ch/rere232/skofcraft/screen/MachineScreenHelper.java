package ch.rere232.skofcraft.screen;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Shared rendering helpers for all mod GUIs.
 * Draws a flat dark theme with an amber accent, without custom textures.
 */
public final class MachineScreenHelper {
    public static final int COLOR_BORDER = 0xFF0F1115;
    public static final int COLOR_PANEL = 0xFF22252B;
    public static final int COLOR_HIGHLIGHT = 0xFF3B414C;
    public static final int COLOR_WELL = 0xFF171A1F;
    public static final int COLOR_ACCENT = 0xFFE0A030;
    public static final int COLOR_PROGRESS = 0xFF3DDC97;
    public static final int COLOR_TEXT = 0xFFE8EAED;
    public static final int COLOR_TEXT_DIM = 0xFF9AA0A6;

    public static final int HEADER_HEIGHT = 18;

    public static final int ENERGY_BAR_X = 152;
    public static final int ENERGY_BAR_Y = 24;
    public static final int ENERGY_BAR_W = 14;
    public static final int ENERGY_BAR_H = 50;

    private MachineScreenHelper() {}

    /** Rectangle with the four corner pixels cut off. */
    public static void fillRounded(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1 + 1, y1, x2 - 1, y2, color);
        g.fill(x1, y1 + 1, x2, y2 - 1, color);
    }

    /** Dark rounded panel with drop shadow, gradient header strip and accent underline. */
    public static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        fillRounded(g, x + 2, y + 2, x + w + 2, y + h + 2, 0x55000000);
        fillRounded(g, x, y, x + w, y + h, COLOR_BORDER);
        fillRounded(g, x + 1, y + 1, x + w - 1, y + h - 1, COLOR_PANEL);

        g.fill(x + 2, y + 1, x + w - 2, y + 2, 0xFF3A404C);
        g.fillGradient(x + 1, y + 2, x + w - 1, y + HEADER_HEIGHT, 0xFF3A404C, 0xFF2B3039);
        g.fill(x + 1, y + HEADER_HEIGHT, x + w - 1, y + HEADER_HEIGHT + 1, COLOR_ACCENT);
    }

    /** Thin two-tone divider line. */
    public static void drawSeparator(GuiGraphics g, int x, int y, int width) {
        g.fill(x, y, x + width, y + 1, COLOR_BORDER);
        g.fill(x, y + 1, x + width, y + 2, COLOR_HIGHLIGHT);
    }

    /** Recessed 18x18 slot background. */
    public static void drawSlot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, COLOR_HIGHLIGHT);
        g.fill(x, y, x + 17, y + 17, COLOR_BORDER);
        g.fill(x + 1, y + 1, x + 17, y + 17, COLOR_WELL);
    }

    /** Accent-framed slot for machine outputs; (x, y) is the inner 18x18 slot origin. */
    public static void drawOutputSlot(GuiGraphics g, int x, int y) {
        fillRounded(g, x - 3, y - 3, x + 21, y + 21, 0xAAE0A030);
        fillRounded(g, x - 2, y - 2, x + 20, y + 20, 0xFF2A2418);
        drawSlot(g, x, y);
    }

    /**
     * Horizontal progress arrow.
     * @param x     left edge of shaft
     * @param y     top of the 18-px-tall arrow area (shaft is centred inside it)
     * @param shaft total shaft width in pixels
     */
    public static void drawArrow(GuiGraphics g, int x, int y, int progress, int maxProgress, int shaft) {
        int total = shaft + 5;
        int fill = (maxProgress > 0 && progress > 0) ? (int) (total * (float) progress / maxProgress) : 0;

        g.fill(x, y + 6, x + shaft, y + 12, COLOR_BORDER);
        g.fill(x + 1, y + 7, x + shaft - 1, y + 11, COLOR_WELL);
        int shaftFill = Math.min(fill, shaft - 2);
        if (shaftFill > 0) {
            g.fillGradient(x + 1, y + 7, x + 1 + shaftFill, y + 11, 0xFF6BF0B8, COLOR_PROGRESS);
        }

        int[] tops = {4, 5, 6, 7, 8};
        int[] bottoms = {14, 13, 12, 11, 10};
        for (int i = 0; i < 5; i++) {
            int color = fill > shaft + i ? COLOR_PROGRESS : COLOR_HIGHLIGHT;
            g.fill(x + shaft + i, y + tops[i], x + shaft + i + 1, y + bottoms[i], color);
        }
    }

    /** Vertical FE energy bar (amber gradient, grows bottom to top), 14x50 pixels. */
    public static void drawFEBar(GuiGraphics g, int x, int y, int energy, int maxEnergy) {
        g.fill(x, y, x + ENERGY_BAR_W, y + ENERGY_BAR_H, COLOR_BORDER);
        g.fill(x + 1, y + 1, x + ENERGY_BAR_W - 1, y + ENERGY_BAR_H - 1, COLOR_WELL);
        if (maxEnergy > 0 && energy > 0) {
            int fill = Math.max(1, (int) (48 * (float) energy / maxEnergy));
            g.fillGradient(x + 1, y + 49 - fill, x + 13, y + 49, 0xFFFFD166, 0xFFE8590C);
        }
        for (int k = 1; k < 4; k++) {
            g.fill(x + 1, y + 1 + k * 12, x + 5, y + 2 + k * 12, 0x66000000);
        }
    }
}
