package ch.rere232.skofcraft.screen;

import ch.rere232.skofcraft.gums.GumSlotData;
import ch.rere232.skofcraft.keybind.SkofcraftKeybinds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GumScreen extends Screen {
    private static final int SLOT_COUNT = 4;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_PADDING = 2;

    private final Player player;

    public GumScreen(Player player) {
        super(Component.literal("Gum Slots"));
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate((this.width - 100) / 2.0, (this.height - 100) / 2.0, 0);

        guiGraphics.fill(0, 0, 100, 100, 0xFF8B8B8B);

        int col = 0, row = 0;
        for (int i = 0; i < SLOT_COUNT; i++) {
            int x = col * (SLOT_SIZE + SLOT_PADDING);
            int y = row * (SLOT_SIZE + SLOT_PADDING);

            guiGraphics.fill(x + 2, y + 2, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF000000);
            guiGraphics.fill(x + 3, y + 3, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF333333);

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
