package ch.rere232.skofcraft.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** An app running inside the phone; it draws into the area below the status bar. */
@OnlyIn(Dist.CLIENT)
public interface PhoneApp {
    void tick();

    void render(GuiGraphics g, Font font, int x, int y, int w, int h, int mouseX, int mouseY);

    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseDragged(double dragY) {
        return false;
    }

    /** Apps that pan in two directions override this; the rest only scroll vertically. */
    default boolean mouseDragged(double dragX, double dragY) {
        return mouseDragged(dragY);
    }

    default void mouseReleased() {}

    default boolean mouseScrolled(double delta) {
        return false;
    }

    default boolean keyPressed(int keyCode) {
        return false;
    }

    /** Called when the player leaves the app or closes the phone. */
    default void onClose() {}
}
