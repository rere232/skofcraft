package ch.rere232.skofcraft.screen;

import ch.rere232.skofcraft.menu.FEDryerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FEDryerScreen extends AbstractMachineScreen<FEDryerMenu> {

    public FEDryerScreen(FEDryerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void drawMachine(GuiGraphics g, int x, int y) {
        MachineScreenHelper.drawArrow(g, x + 64, y + 35, menu.getProgress(), Math.max(menu.getMaxProgress(), 1), 40);
    }

    @Override
    protected boolean usesEnergy() {
        return menu.requiresEnergy();
    }

    @Override
    protected int energy() {
        return menu.getEnergy();
    }

    @Override
    protected int maxEnergy() {
        return menu.getMaxEnergy();
    }
}
