package ch.rere232.skofcraft.screen;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Shared rendering helpers for all machine GUIs.
 * Produces a vanilla-style MC panel appearance without custom textures.
 */
public final class MachineScreenHelper {
    private MachineScreenHelper() {}

    /** Light gray panel with 1-px raised border, matching vanilla inventory appearance. */
    public static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFFC6C6C6);
        // Highlight edges (top, left)
        g.fill(x, y, x + w, y + 1, 0xFFFFFFFF);
        g.fill(x, y, x + 1, y + h, 0xFFFFFFFF);
        // Shadow edges (bottom, right)
        g.fill(x, y + h - 1, x + w, y + h, 0xFF555555);
        g.fill(x + w - 1, y, x + w, y + h, 0xFF555555);
    }

    /** Two-pixel separator line between machine area and player inventory. */
    public static void drawSeparator(GuiGraphics g, int x, int y, int width) {
        g.fill(x, y, x + width, y + 1, 0xFF8B8B8B);
        g.fill(x, y + 1, x + width, y + 2, 0xFFFFFFFF);
    }

    /** Recessed 18×18 slot background matching vanilla slot appearance. */
    public static void drawSlot(GuiGraphics g, int x, int y) {
        // Dark edges top/left (inset look)
        g.fill(x, y, x + 18, y + 1, 0xFF373737);
        g.fill(x, y + 1, x + 1, y + 17, 0xFF373737);
        // Light edges bottom/right
        g.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 17, y + 1, x + 18, y + 17, 0xFFFFFFFF);
        // Interior
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

    /**
     * Horizontal progress arrow.
     * @param x     left edge of shaft
     * @param y     top of the 18-px-tall arrow area (shaft is centred inside it)
     * @param shaft total shaft width in pixels
     */
    public static void drawArrow(GuiGraphics g, int x, int y, int progress, int maxProgress, int shaft) {
        // Shaft background
        g.fill(x,     y + 5, x + shaft,     y + 13, 0xFF373737);
        g.fill(x + 1, y + 6, x + shaft - 1, y + 12, 0xFF555555);
        // Green fill
        if (maxProgress > 0 && progress > 0) {
            int fill = (int)((shaft - 2) * (float) progress / maxProgress);
            if (fill > 0) g.fill(x + 1, y + 6, x + 1 + fill, y + 12, 0xFF00AA00);
        }
        // Arrowhead (five diminishing vertical bars → right triangle)
        g.fill(x + shaft,     y + 4, x + shaft + 1, y + 14, 0xFF373737);
        g.fill(x + shaft + 1, y + 5, x + shaft + 2, y + 13, 0xFF373737);
        g.fill(x + shaft + 2, y + 6, x + shaft + 3, y + 12, 0xFF373737);
        g.fill(x + shaft + 3, y + 7, x + shaft + 4, y + 11, 0xFF373737);
        g.fill(x + shaft + 4, y + 8, x + shaft + 5, y + 10, 0xFF373737);
    }

    /**
     * Vertical FE energy bar (orange fill, grows bottom→top).
     * Occupies 14×50 pixels at the given position.
     */
    public static void drawFEBar(GuiGraphics g, int x, int y, int energy, int maxEnergy) {
        // Border
        g.fill(x, y, x + 14, y + 50, 0xFF373737);
        // Background
        g.fill(x + 1, y + 1, x + 13, y + 49, 0xFF0A0A0A);
        if (maxEnergy > 0 && energy > 0) {
            int fill = (int)(48 * (float) energy / maxEnergy);
            g.fill(x + 1, y + 49 - fill, x + 13, y + 49, 0xFFFFAA00);
        }
    }
}
