package ch.rere232.skofcraft.screen;

import ch.rere232.skofcraft.blockentity.FEDryerBlockEntity;
import ch.rere232.skofcraft.menu.FEDryerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FEDryerScreen extends AbstractContainerScreen<FEDryerMenu> {
    private static final int TEXTURE_WIDTH = 176;
    private static final int TEXTURE_HEIGHT = 222;

    public FEDryerScreen(FEDryerMenu menu, Inventory inventory, net.minecraft.network.chat.Component component) {
        super(menu, inventory, component);
        this.imageWidth = TEXTURE_WIDTH;
        this.imageHeight = TEXTURE_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF8B8B8B);

        FEDryerBlockEntity be = this.menu.getBlockEntity();
        if (be != null) {
            int progress = be.getProcessingProgress();
            int maxProgress = be.getMaxProgress();
            int barWidth = (int) (progress * 40.0F / maxProgress);
            guiGraphics.fill(this.leftPos + 85, this.topPos + 40, this.leftPos + 85 + barWidth, this.topPos + 48, 0xFF00FF00);

            int energy = be.getEnergy().getEnergyStored();
            int maxEnergy = be.getEnergy().getMaxEnergyStored();
            int energyBarHeight = (int) (energy * 48.0F / maxEnergy);
            guiGraphics.fill(this.leftPos + 20, this.topPos + 60 + (48 - energyBarHeight), this.leftPos + 26, this.topPos + 60 + 48, 0xFFFFFF00);
        }
    }
}
