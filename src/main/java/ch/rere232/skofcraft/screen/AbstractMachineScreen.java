package ch.rere232.skofcraft.screen;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Base for all machine screens: themed panel, slot backgrounds derived from the menu,
 * light labels, and an optional FE bar with tooltip.
 */
@OnlyIn(Dist.CLIENT)
public abstract class AbstractMachineScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private final Inventory playerInventory;

    protected AbstractMachineScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.playerInventory = inventory;
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 8;
        this.titleLabelY = 5;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 73;
    }

    /** Draws machine-specific widgets such as the progress arrow. */
    protected abstract void drawMachine(GuiGraphics g, int x, int y);

    protected boolean usesEnergy() {
        return false;
    }

    protected int energy() {
        return 0;
    }

    protected int maxEnergy() {
        return 0;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
        if (usesEnergy()) {
            renderEnergyTooltip(g, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos, y = this.topPos;
        MachineScreenHelper.drawPanel(g, x, y, this.imageWidth, this.imageHeight);

        Slot lastMachineSlot = null;
        for (Slot slot : this.menu.slots) {
            if (slot.container != playerInventory) {
                lastMachineSlot = slot;
            }
        }
        for (Slot slot : this.menu.slots) {
            if (slot == lastMachineSlot) {
                MachineScreenHelper.drawOutputSlot(g, x + slot.x - 1, y + slot.y - 1);
            } else {
                MachineScreenHelper.drawSlot(g, x + slot.x - 1, y + slot.y - 1);
            }
        }

        drawMachine(g, x, y);
        if (usesEnergy()) {
            MachineScreenHelper.drawFEBar(g, x + MachineScreenHelper.ENERGY_BAR_X, y + MachineScreenHelper.ENERGY_BAR_Y,
                    energy(), maxEnergy());
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, MachineScreenHelper.COLOR_TEXT, false);
        g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                MachineScreenHelper.COLOR_TEXT_DIM, false);
    }

    private void renderEnergyTooltip(GuiGraphics g, int mouseX, int mouseY) {
        int bx = this.leftPos + MachineScreenHelper.ENERGY_BAR_X;
        int by = this.topPos + MachineScreenHelper.ENERGY_BAR_Y;
        if (mouseX < bx || mouseX >= bx + MachineScreenHelper.ENERGY_BAR_W
                || mouseY < by || mouseY >= by + MachineScreenHelper.ENERGY_BAR_H) {
            return;
        }
        int max = maxEnergy();
        int percent = max > 0 ? (int) (100L * energy() / max) : 0;
        g.renderComponentTooltip(this.font, List.of(
                Component.literal("Energy"),
                Component.literal(String.format("%,d / %,d FE (%d%%)", energy(), max, percent))
        ), mouseX, mouseY);
    }
}
