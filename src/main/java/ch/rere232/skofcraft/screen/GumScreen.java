package ch.rere232.skofcraft.screen;

import ch.rere232.skofcraft.gums.GumSlotData;
import ch.rere232.skofcraft.item.GumConsumableItem;
import ch.rere232.skofcraft.keybind.SkofcraftKeybinds;
import ch.rere232.skofcraft.network.GumRemoveC2SPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GumScreen extends Screen {
    private static final int SLOT_COUNT = 4;
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 219;
    private static final int GUM_SLOT_X = 8;
    private static final int GUM_SLOT_Y = 23;
    private static final int STATS_Y = 99;
    private static final int STATS_BAR_W = 76;
    private static final int STATS_GAP = 8;
    private static final int HINT_Y = 117;
    private static final int INVENTORY_LABEL_Y = 129;
    private static final int INVENTORY_Y = 139;
    private static final int HOTBAR_Y = 197;

    private final Player player;
    private int leftPos;
    private int topPos;

    public GumScreen(Player player) {
        super(Component.literal("Gum Slots"));
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (this.width - PANEL_WIDTH) / 2;
        topPos = (this.height - PANEL_HEIGHT) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        MachineScreenHelper.drawPanel(guiGraphics, leftPos, topPos, PANEL_WIDTH, PANEL_HEIGHT);

        guiGraphics.drawString(this.font, this.title, leftPos + 8, topPos + 5, MachineScreenHelper.COLOR_TEXT, false);

        GumSlotData gumData = GumSlotData.get(player);
        ItemStack hovered = ItemStack.EMPTY;
        boolean hoveredIsGumSlot = false;

        for (int i = 0; i < SLOT_COUNT; i++) {
            int x = leftPos + GUM_SLOT_X;
            int y = topPos + GUM_SLOT_Y + i * 18;
            MachineScreenHelper.drawSlot(guiGraphics, x, y);

            ItemStack slotItem = gumData.getSlotItem(i);
            int remainingTicks = gumData.getRemainingTicks(i);
            int textX = x + 22;
            int textRight = leftPos + PANEL_WIDTH - 8;

            if (!slotItem.isEmpty() && remainingTicks > 0) {
                guiGraphics.renderItem(slotItem, x + 1, y + 1);

                String time = formatTime(remainingTicks);
                int timeWidth = this.font.width(time);
                String name = this.font.plainSubstrByWidth(slotItem.getHoverName().getString(), textRight - textX - timeWidth - 4);
                guiGraphics.drawString(this.font, name, textX, y + 1, MachineScreenHelper.COLOR_TEXT, false);
                guiGraphics.drawString(this.font, time, textRight - timeWidth, y + 1, MachineScreenHelper.COLOR_TEXT_DIM, false);

                int barWidth = textRight - textX;
                float ratio = timeRatio(slotItem, remainingTicks);
                guiGraphics.fill(textX, y + 11, textX + barWidth, y + 15, MachineScreenHelper.COLOR_BORDER);
                guiGraphics.fill(textX + 1, y + 12, textX + barWidth - 1, y + 14, MachineScreenHelper.COLOR_WELL);
                int filled = Math.max(1, (int) ((barWidth - 2) * ratio));
                guiGraphics.fill(textX + 1, y + 12, textX + 1 + filled, y + 14, barColor(ratio));
            } else {
                guiGraphics.drawString(this.font, "Empty", textX, y + 5, MachineScreenHelper.COLOR_TEXT_DIM, false);
            }

            if (inside(mouseX, mouseY, x, y)) {
                if (!slotItem.isEmpty()) {
                    hovered = slotItem;
                    hoveredIsGumSlot = true;
                }
                renderSlotHover(guiGraphics, x, y);
            }
        }

        guiGraphics.drawString(this.font, Component.literal("Right-click gum item to equip"), leftPos + 8, topPos + HINT_Y, MachineScreenHelper.COLOR_TEXT_DIM, false);
        guiGraphics.drawString(this.font, Component.literal("Inventory"), leftPos + 8, topPos + INVENTORY_LABEL_Y, MachineScreenHelper.COLOR_TEXT, false);

        int tolerance = gumData.getTolerance();
        int addiction = gumData.getAddiction();
        drawStatBar(guiGraphics, leftPos + 8, topPos + STATS_Y, "Tolerance", tolerance, GumSlotData.MAX_TOLERANCE, 0xFF6CB8F5, 0xFF3A7FC4);
        int addictionColor = addictionColor(addiction / (float) GumSlotData.MAX_ADDICTION);
        drawStatBar(guiGraphics, leftPos + 8 + STATS_BAR_W + STATS_GAP, topPos + STATS_Y, "Addiction", addiction, GumSlotData.MAX_ADDICTION, addictionColor, darken(addictionColor));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = leftPos + 8 + col * 18;
                int slotY = topPos + INVENTORY_Y + row * 18;
                ItemStack stack = player.getInventory().getItem(col + row * 9 + 9);
                if (drawInventorySlot(guiGraphics, stack, slotX, slotY, mouseX, mouseY)) {
                    hovered = stack;
                    hoveredIsGumSlot = false;
                }
            }
        }

        for (int col = 0; col < 9; col++) {
            int slotX = leftPos + 8 + col * 18;
            int slotY = topPos + HOTBAR_Y;
            ItemStack stack = player.getInventory().getItem(col);
            if (drawInventorySlot(guiGraphics, stack, slotX, slotY, mouseX, mouseY)) {
                hovered = stack;
                hoveredIsGumSlot = false;
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (!hovered.isEmpty()) {
            List<Component> lines = new ArrayList<>(this.getTooltipFromItem(this.minecraft, hovered));
            if (hoveredIsGumSlot) {
                lines.add(Component.literal("Right-click to remove").withStyle(ChatFormatting.GRAY));
            }
            guiGraphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        } else {
            renderStatTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    private void drawStatBar(GuiGraphics g, int x, int y, String label, int value, int max, int colorTop, int colorBottom) {
        int percent = Math.max(0, Math.min(100, value * 100 / max));
        g.drawString(this.font, label, x, y, MachineScreenHelper.COLOR_TEXT, false);
        String pct = percent + "%";
        g.drawString(this.font, pct, x + STATS_BAR_W - this.font.width(pct), y, MachineScreenHelper.COLOR_TEXT_DIM, false);

        int barY = y + 10;
        g.fill(x, barY, x + STATS_BAR_W, barY + 6, MachineScreenHelper.COLOR_BORDER);
        g.fill(x + 1, barY + 1, x + STATS_BAR_W - 1, barY + 5, MachineScreenHelper.COLOR_WELL);
        int filled = (STATS_BAR_W - 2) * percent / 100;
        if (percent > 0) {
            g.fillGradient(x + 1, barY + 1, x + 1 + Math.max(1, filled), barY + 5, colorTop, colorBottom);
        }
    }

    private void renderStatTooltip(GuiGraphics g, int mouseX, int mouseY) {
        int y = topPos + STATS_Y;
        if (mouseY < y || mouseY >= y + 16) {
            return;
        }
        int tolX = leftPos + 8;
        int addX = tolX + STATS_BAR_W + STATS_GAP;
        List<Component> lines = null;
        if (mouseX >= tolX && mouseX < tolX + STATS_BAR_W) {
            lines = List.of(
                    Component.literal("Tolerance"),
                    Component.literal("Weakens pouch bonuses.").withStyle(ChatFormatting.GRAY),
                    Component.literal("Rises with use, falls when you stop.").withStyle(ChatFormatting.GRAY));
        } else if (mouseX >= addX && mouseX < addX + STATS_BAR_W) {
            lines = List.of(
                    Component.literal("Addiction"),
                    Component.literal("Withdrawal effects appear when you").withStyle(ChatFormatting.GRAY),
                    Component.literal("go without a pouch for too long.").withStyle(ChatFormatting.GRAY));
        }
        if (lines != null) {
            g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }
    }

    private static int addictionColor(float ratio) {
        if (ratio > 0.7f) return 0xFFE5484D;
        if (ratio > 0.35f) return 0xFFEE7B30;
        return MachineScreenHelper.COLOR_ACCENT;
    }

    private static int darken(int color) {
        int r = ((color >> 16) & 0xFF) * 3 / 4;
        int gr = ((color >> 8) & 0xFF) * 3 / 4;
        int b = (color & 0xFF) * 3 / 4;
        return 0xFF000000 | (r << 16) | (gr << 8) | b;
    }

    private boolean drawInventorySlot(GuiGraphics g, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        MachineScreenHelper.drawSlot(g, x, y);
        if (!stack.isEmpty()) {
            g.renderItem(stack, x + 1, y + 1);
            g.renderItemDecorations(this.font, stack, x + 1, y + 1);
        }
        boolean over = inside(mouseX, mouseY, x, y);
        if (over) {
            renderSlotHover(g, x, y);
        }
        return over && !stack.isEmpty();
    }

    private static void renderSlotHover(GuiGraphics g, int x, int y) {
        g.fill(x + 1, y + 1, x + 17, y + 17, 0x40FFFFFF);
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
    }

    private static float timeRatio(ItemStack stack, int remainingTicks) {
        if (stack.getItem() instanceof GumConsumableItem consumable) {
            int total = consumable.getDurationMinutes() * 60 * 20;
            if (total > 0) {
                return Math.min(1f, (float) remainingTicks / total);
            }
        }
        return 1f;
    }

    private static int barColor(float ratio) {
        if (ratio > 0.5f) return MachineScreenHelper.COLOR_PROGRESS;
        if (ratio > 0.2f) return MachineScreenHelper.COLOR_ACCENT;
        return 0xFFE5484D;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX;
        int y = (int) mouseY;
        int slotBaseX = leftPos + GUM_SLOT_X;
        int slotBaseY = topPos + GUM_SLOT_Y;

        if (button == 1) {
            for (int i = 0; i < SLOT_COUNT; i++) {
                int sx = slotBaseX;
                int sy = slotBaseY + i * 18;
                if (x >= sx && x < sx + 18 && y >= sy && y < sy + 18) {
                    GumSlotData gumData = GumSlotData.get(player);
                    if (!gumData.getSlotItem(i).isEmpty()) {
                        gumData.removeSlot(i);
                        SkofcraftNetwork.CHANNEL.sendToServer(new GumRemoveC2SPacket(i));
                    }
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == SkofcraftKeybinds.OPEN_GUMS_SCREEN.getKey().getValue()) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private String formatTime(int ticks) {
        int totalSeconds = Math.max(0, ticks / 20);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
