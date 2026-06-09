package ch.rere232.skofcraft.screen;

import ch.rere232.skofcraft.gums.GumSlotData;
import ch.rere232.skofcraft.keybind.SkofcraftKeybinds;
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
    private static final int PANEL_HEIGHT = 166;

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
        MachineScreenHelper.drawSeparator(guiGraphics, leftPos + 7, topPos + 82, 162);

        guiGraphics.drawString(this.font, this.title, leftPos + 8, topPos + 6, 0x404040, false);
        guiGraphics.drawString(this.font, Component.literal("Inventory"), leftPos + 8, topPos + 72, 0x404040, false);
        guiGraphics.drawString(this.font, Component.literal("Right-click gum item to equip"), leftPos + 8, topPos + 58, 0x404040, false);

        GumSlotData gumData = GumSlotData.get(player);

        int baseX = leftPos + 44;
        int baseY = topPos + 20;
        for (int i = 0; i < SLOT_COUNT; i++) {
            int x = baseX + i * 18;
            int y = baseY;
            MachineScreenHelper.drawSlot(guiGraphics, x, y);

            ItemStack slotItem = gumData.getSlotItem(i);
            if (!slotItem.isEmpty()) {
                guiGraphics.renderItem(slotItem, x + 1, y + 1);
                guiGraphics.renderItemDecorations(this.font, slotItem, x + 1, y + 1);
                int remainingTicks = gumData.getRemainingTicks(i);
                if (remainingTicks > 0) {
                    int remainingSeconds = Math.max(1, remainingTicks / 20);
                    guiGraphics.drawString(this.font, String.valueOf(remainingSeconds), x + 11, y + 11, 0xFFFFFF, false);
                }
            }
        }

        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack slotItem = gumData.getSlotItem(i);
            int remainingTicks = gumData.getRemainingTicks(i);
            String line;
            int color;

            if (!slotItem.isEmpty() && remainingTicks > 0) {
                line = "#" + (i + 1) + " " + slotItem.getHoverName().getString() + " - " + formatTime(remainingTicks);
                color = 0x202020;
            } else {
                line = "#" + (i + 1) + " Empty";
                color = 0x606060;
            }

            guiGraphics.drawString(this.font, line, leftPos + 8, topPos + 48 + i * 9, color, false);
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = leftPos + 8 + col * 18;
                int slotY = topPos + 84 + row * 18;
                MachineScreenHelper.drawSlot(guiGraphics, slotX, slotY);
                ItemStack stack = player.getInventory().getItem(col + row * 9 + 9);
                if (!stack.isEmpty()) {
                    guiGraphics.renderItem(stack, slotX + 1, slotY + 1);
                    guiGraphics.renderItemDecorations(this.font, stack, slotX + 1, slotY + 1);
                }
            }
        }

        for (int col = 0; col < 9; col++) {
            int slotX = leftPos + 8 + col * 18;
            int slotY = topPos + 142;
            MachineScreenHelper.drawSlot(guiGraphics, slotX, slotY);
            ItemStack stack = player.getInventory().getItem(col);
            if (!stack.isEmpty()) {
                guiGraphics.renderItem(stack, slotX + 1, slotY + 1);
                guiGraphics.renderItemDecorations(this.font, stack, slotX + 1, slotY + 1);
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX;
        int y = (int) mouseY;
        int slotBaseX = leftPos + 44;
        int slotBaseY = topPos + 20;

        if (button == 1) {
            for (int i = 0; i < SLOT_COUNT; i++) {
                int sx = slotBaseX + i * 18;
                int sy = slotBaseY;
                if (x >= sx && x < sx + 18 && y >= sy && y < sy + 18) {
                    GumSlotData gumData = GumSlotData.get(player);
                    ItemStack slotItem = gumData.getSlotItem(i);
                    if (!slotItem.isEmpty()) {
                        gumData.removeSlot(i);
                        player.addItem(slotItem.copy());
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
