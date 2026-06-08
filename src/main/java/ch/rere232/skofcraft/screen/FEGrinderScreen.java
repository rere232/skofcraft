package ch.rere232.skofcraft.screen;

import ch.rere232.skofcraft.blockentity.FEGrinderBlockEntity;
import ch.rere232.skofcraft.menu.FEGrinderMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;

@OnlyIn(Dist.CLIENT)
public class FEGrinderScreen extends AbstractContainerScreen<FEGrinderMenu> {
    private static final int TEXTURE_WIDTH = 176;
    private static final int TEXTURE_HEIGHT = 222;

    public FEGrinderScreen(FEGrinderMenu menu, Inventory inventory, net.minecraft.network.chat.Component component) {
        super(menu, inventory, component);
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
    }

    @Override
    public void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF8B8B8B);

        FEGrinderBlockEntity blockEntity = menu.getBlockEntity();
        int energy = blockEntity.getEnergy().getEnergyStored();
        int maxEnergy = blockEntity.getEnergy().getMaxEnergyStored();
        int progress = blockEntity.getProcessingProgress();
        int maxProgress = blockEntity.getMaxProgress();

        int energyBarHeight = (int) (48.0F * energy / maxEnergy);
        guiGraphics.fill(this.leftPos + 152, this.topPos + 88 - energyBarHeight, this.leftPos + 166, this.topPos + 88, 0xFFFFFF00);

        int progressWidth = (int) (40.0F * progress / maxProgress);
        guiGraphics.fill(this.leftPos + 79, this.topPos + 35, this.leftPos + 79 + progressWidth, this.topPos + 48, 0xFF00FF00);
    }
}
