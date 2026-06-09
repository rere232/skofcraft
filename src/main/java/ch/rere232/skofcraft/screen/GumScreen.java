package ch.rere232.skofcraft.screen;

import ch.rere232.skofcraft.gums.GumSlotData;
import ch.rere232.skofcraft.keybind.SkofcraftKeybinds;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.client.Minecraft;
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
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_PADDING = 2;

    private final Player player;
    private int offsetX;
    private int offsetY;

    public GumScreen(Player player) {
        super(Component.literal("Gum Slots"));
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();
        offsetX = (this.width - 100) / 2;
        offsetY = (this.height - 100) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(offsetX, offsetY, 0);

        guiGraphics.fill(0, 0, 100, 100, 0xFF8B8B8B);

        GumSlotData gumData = GumSlotData.get(player);

        int col = 0, row = 0;
        for (int i = 0; i < SLOT_COUNT; i++) {
            int x = col * (SLOT_SIZE + SLOT_PADDING);
            int y = row * (SLOT_SIZE + SLOT_PADDING);

            guiGraphics.fill(x + 2, y + 2, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF000000);
            guiGraphics.fill(x + 3, y + 3, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF333333);

            ItemStack slotItem = gumData.getSlotItem(i);
            if (!slotItem.isEmpty()) {
                guiGraphics.renderItem(slotItem, x + 3, y + 3);
                guiGraphics.renderItemDecorations(this.font, slotItem, x + 3, y + 3);
                int remainingTicks = gumData.getRemainingTicks(i);
                if (remainingTicks > 0) {
                    int remainingSeconds = Math.max(1, remainingTicks / 20);
                    guiGraphics.drawString(this.font, String.valueOf(remainingSeconds), x + 8, y + 8, 0xFFFFFF);
                }
            }

            col++;
            if (col >= 2) {
                col = 0;
                row++;
            }
        }

        guiGraphics.pose().popPose();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int relX = (int) (mouseX - offsetX);
        int relY = (int) (mouseY - offsetY);

        if (relX >= 0 && relX <= 100 && relY >= 0 && relY <= 100) {
            int col = relX / (SLOT_SIZE + SLOT_PADDING);
            int row = relY / (SLOT_SIZE + SLOT_PADDING);
            if (col >= 2) col = 1;
            if (row >= 2) row = 1;
            int slotIndex = col + row * 2;

            if (slotIndex < SLOT_COUNT) {
                GumSlotData gumData = GumSlotData.get(player);
                ItemStack slotItem = gumData.getSlotItem(slotIndex);
                
                if (!slotItem.isEmpty()) {
                    gumData.removeSlot(slotIndex);
                    return true;
                }
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isGumItem(Object item) {
        return item == SkofcraftItems.SNUS_POUCH.get() || item == SkofcraftItems.NICOTINE_POUCH.get();
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
}
