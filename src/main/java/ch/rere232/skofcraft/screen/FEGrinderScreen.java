package ch.rere232.skofcraft.screen;

import ch.rere232.skofcraft.blockentity.FEGrinderBlockEntity;
import ch.rere232.skofcraft.menu.FEGrinderMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FEGrinderScreen extends AbstractContainerScreen<FEGrinderMenu> {

    public FEGrinderScreen(FEGrinderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos, y = this.topPos;
        MachineScreenHelper.drawPanel(g, x, y, 176, 166);
        MachineScreenHelper.drawSeparator(g, x + 7, y + 82, 162);
        MachineScreenHelper.drawSlot(g, x + 44, y + 35);
        MachineScreenHelper.drawSlot(g, x + 116, y + 35);
        FEGrinderBlockEntity be = this.menu.getBlockEntity();
        if (be == null) return;
        MachineScreenHelper.drawArrow(g, x + 64, y + 26, be.getProcessingProgress(), Math.max(be.getMaxProgress(), 1), 48);
        MachineScreenHelper.drawFEBar(g, x + 152, y + 14, be.getEnergy().getEnergyStored(), be.getEnergy().getMaxEnergyStored());
    }
}
