package ch.rere232.skofcraft.screen;

import ch.rere232.skofcraft.screen.QuestBookContent.Chapter;
import ch.rere232.skofcraft.screen.QuestBookContent.Craft;
import ch.rere232.skofcraft.screen.QuestBookContent.Entry;
import ch.rere232.skofcraft.screen.QuestBookContent.Process;
import ch.rere232.skofcraft.screen.QuestBookContent.Text;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Guide book: chapter list on the left, scrollable explanations and recipes on the right. */
@OnlyIn(Dist.CLIENT)
public class QuestBookScreen extends Screen {
    private static final int PANEL_W = 300;
    private static final int PANEL_H = 196;
    private static final int SIDEBAR_W = 88;
    private static final int ROW_H = 16;
    private static final int TOP_PAD = 24;
    private static final int CONTENT_X = SIDEBAR_W + 12;
    private static final int CONTENT_W = PANEL_W - CONTENT_X - 12;
    private static final int VIEW_H = PANEL_H - TOP_PAD - 8;
    private static final int LINE_H = 9;
    private static final int BLOCK_GAP = 6;

    private static int rememberedChapter = 0;

    private final List<Chapter> chapters = QuestBookContent.build();
    private int selected = Math.min(rememberedChapter, chapters.size() - 1);
    private int leftPos;
    private int topPos;
    private int scroll;
    private int sidebarScroll;
    private int contentHeight;
    private ItemStack hovered = ItemStack.EMPTY;

    public QuestBookScreen() {
        super(Component.translatable("questbook.skofcraft.heading"));
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (this.width - PANEL_W) / 2;
        topPos = (this.height - PANEL_H) / 2;
        sidebarScroll = Mth.clamp(selected * ROW_H - VIEW_H / 2 + ROW_H, 0, sidebarMax());
    }

    private int sidebarMax() {
        return Math.max(0, chapters.size() * ROW_H + 4 - VIEW_H);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        MachineScreenHelper.drawPanel(g, leftPos, topPos, PANEL_W, PANEL_H);
        g.drawString(this.font, this.title, leftPos + 8, topPos + 5, MachineScreenHelper.COLOR_TEXT, false);

        hovered = ItemStack.EMPTY;
        renderSidebar(g, mouseX, mouseY);
        renderContent(g, mouseX, mouseY);

        super.render(g, mouseX, mouseY, partialTick);

        if (!hovered.isEmpty()) {
            g.renderComponentTooltip(this.font, this.getTooltipFromItem(this.minecraft, hovered), mouseX, mouseY);
        }
    }

    private void renderSidebar(GuiGraphics g, int mouseX, int mouseY) {
        int x = leftPos + 6;
        int y = topPos + TOP_PAD;
        MachineScreenHelper.fillRounded(g, x, y, x + SIDEBAR_W, y + VIEW_H, MachineScreenHelper.COLOR_WELL);

        g.enableScissor(x, y, x + SIDEBAR_W, y + VIEW_H);
        boolean mouseInSidebar = mouseX >= x && mouseX < x + SIDEBAR_W && mouseY >= y && mouseY < y + VIEW_H;
        for (int i = 0; i < chapters.size(); i++) {
            int rowY = y + 2 + i * ROW_H - sidebarScroll;
            if (rowY + ROW_H < y || rowY > y + VIEW_H) {
                continue;
            }
            boolean over = mouseInSidebar && mouseY >= rowY && mouseY < rowY + ROW_H;
            if (i == selected) {
                g.fill(x + 2, rowY, x + SIDEBAR_W - 2, rowY + ROW_H, 0xFF2B3039);
                g.fill(x + 2, rowY, x + 4, rowY + ROW_H, MachineScreenHelper.COLOR_ACCENT);
            } else if (over) {
                g.fill(x + 2, rowY, x + SIDEBAR_W - 2, rowY + ROW_H, 0xFF20242B);
            }

            g.pose().pushPose();
            g.pose().translate(x + 7, rowY + 2, 0);
            g.pose().scale(0.75f, 0.75f, 1f);
            g.renderItem(chapters.get(i).icon(), 0, 0);
            g.pose().popPose();

            String title = this.font.plainSubstrByWidth(chapters.get(i).title().getString(), SIDEBAR_W - 28);
            int color = i == selected ? MachineScreenHelper.COLOR_TEXT : MachineScreenHelper.COLOR_TEXT_DIM;
            g.drawString(this.font, title, x + 22, rowY + 4, color, false);
        }
        g.disableScissor();

        int max = sidebarMax();
        if (max > 0) {
            int thumbH = Math.max(12, VIEW_H * VIEW_H / (VIEW_H + max));
            int thumbY = y + (VIEW_H - thumbH) * sidebarScroll / max;
            g.fill(x + SIDEBAR_W - 3, y + 2, x + SIDEBAR_W - 1, y + VIEW_H - 2, MachineScreenHelper.COLOR_BORDER);
            g.fill(x + SIDEBAR_W - 3, thumbY, x + SIDEBAR_W - 1, thumbY + thumbH, MachineScreenHelper.COLOR_ACCENT);
        }
    }

    private void renderContent(GuiGraphics g, int mouseX, int mouseY) {
        int viewX = leftPos + CONTENT_X;
        int viewY = topPos + TOP_PAD;
        MachineScreenHelper.fillRounded(g, viewX - 4, viewY, viewX + CONTENT_W + 6, viewY + VIEW_H, MachineScreenHelper.COLOR_WELL);

        Chapter chapter = chapters.get(selected);
        g.enableScissor(viewX - 4, viewY, viewX + CONTENT_W + 6, viewY + VIEW_H);

        int cursor = viewY + 4 - scroll;
        g.drawString(this.font, chapter.title(), viewX, cursor, MachineScreenHelper.COLOR_ACCENT, false);
        cursor += LINE_H + 2;
        g.fill(viewX, cursor, viewX + CONTENT_W - 6, cursor + 1, MachineScreenHelper.COLOR_HIGHLIGHT);
        cursor += 6;

        boolean mouseInView = mouseX >= viewX - 4 && mouseX < viewX + CONTENT_W + 6 && mouseY >= viewY && mouseY < viewY + VIEW_H;
        for (Entry entry : chapter.entries()) {
            if (entry instanceof Text text) {
                List<FormattedCharSequence> lines = this.font.split(text.text(), CONTENT_W - 6);
                for (FormattedCharSequence line : lines) {
                    g.drawString(this.font, line, viewX, cursor, 0xFFD5D8DD, false);
                    cursor += LINE_H;
                }
                cursor += BLOCK_GAP;
            } else if (entry instanceof Craft craft) {
                cursor = drawCraft(g, craft, viewX, cursor, mouseX, mouseY, mouseInView);
            } else if (entry instanceof Process process) {
                cursor = drawProcess(g, process, viewX, cursor, mouseX, mouseY, mouseInView);
            }
        }
        g.disableScissor();

        contentHeight = cursor + scroll - viewY + 2;
        int maxScroll = Math.max(0, contentHeight - VIEW_H);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        if (maxScroll > 0) {
            int trackX = viewX + CONTENT_W + 1;
            int thumbH = Math.max(12, VIEW_H * VIEW_H / contentHeight);
            int thumbY = viewY + (VIEW_H - thumbH) * scroll / maxScroll;
            g.fill(trackX, viewY + 2, trackX + 3, viewY + VIEW_H - 2, MachineScreenHelper.COLOR_BORDER);
            g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, MachineScreenHelper.COLOR_ACCENT);
        }
    }

    private int drawCraft(GuiGraphics g, Craft craft, int x, int y, int mouseX, int mouseY, boolean mouseInView) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                drawItemSlot(g, craft.grid()[row * 3 + col], x + col * 18, y + row * 18, mouseX, mouseY, mouseInView);
            }
        }
        MachineScreenHelper.drawArrow(g, x + 62, y + 18, 1, 1, 8);
        int resultX = x + 62 + 13 + 8;
        MachineScreenHelper.drawOutputSlot(g, resultX, y + 18);
        drawItem(g, craft.result(), resultX, y + 18, mouseX, mouseY, mouseInView);
        return y + 54 + BLOCK_GAP;
    }

    private int drawProcess(GuiGraphics g, Process process, int x, int y, int mouseX, int mouseY, boolean mouseInView) {
        int cx = x;
        List<ItemStack> inputs = process.inputs();
        for (int i = 0; i < inputs.size(); i++) {
            if (i > 0) {
                g.drawString(this.font, "+", cx + 1, y + 5, MachineScreenHelper.COLOR_TEXT_DIM, false);
                cx += 8;
            }
            drawItemSlot(g, inputs.get(i), cx, y, mouseX, mouseY, mouseInView);
            cx += 18;
        }
        MachineScreenHelper.drawArrow(g, cx + 3, y, 1, 1, 6);
        cx += 3 + 11 + 3;
        MachineScreenHelper.drawSlot(g, cx, y);
        drawItem(g, process.machine(), cx, y, mouseX, mouseY, mouseInView);
        cx += 18;
        MachineScreenHelper.drawArrow(g, cx + 3, y, 1, 1, 6);
        cx += 3 + 11 + 3;
        MachineScreenHelper.drawOutputSlot(g, cx, y);
        drawItem(g, process.output(), cx, y, mouseX, mouseY, mouseInView);
        return y + 18 + BLOCK_GAP + 2;
    }

    private void drawItemSlot(GuiGraphics g, ItemStack stack, int x, int y, int mouseX, int mouseY, boolean mouseInView) {
        MachineScreenHelper.drawSlot(g, x, y);
        drawItem(g, stack, x, y, mouseX, mouseY, mouseInView);
    }

    private void drawItem(GuiGraphics g, ItemStack stack, int x, int y, int mouseX, int mouseY, boolean mouseInView) {
        if (stack.isEmpty()) {
            return;
        }
        g.renderItem(stack, x + 1, y + 1);
        g.renderItemDecorations(this.font, stack, x + 1, y + 1);
        if (mouseInView && mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
            g.fill(x + 1, y + 1, x + 17, y + 17, 0x40FFFFFF);
            hovered = stack;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = leftPos + 6;
        int y = topPos + TOP_PAD + 2;
        if (button == 0 && mouseX >= x && mouseX < x + SIDEBAR_W && mouseY >= topPos + TOP_PAD && mouseY < topPos + TOP_PAD + VIEW_H) {
            int index = (int) ((mouseY - y + sidebarScroll) / ROW_H);
            if (index >= 0 && index < chapters.size()) {
                selectChapter(index);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < leftPos + 6 + SIDEBAR_W + 3) {
            sidebarScroll = (int) Mth.clamp(sidebarScroll - delta * ROW_H, 0, sidebarMax());
            return true;
        }
        scroll = (int) Math.max(0, Math.min(scroll - delta * 14, Math.max(0, contentHeight - VIEW_H)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void selectChapter(int index) {
        selected = index;
        rememberedChapter = index;
        scroll = 0;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
