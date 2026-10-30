package ch.rere232.skofcraft.screen;

import ch.rere232.skofcraft.config.SkofcraftConfig;
import ch.rere232.skofcraft.menu.FEPackagerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FEPackagerScreen extends AbstractMachineScreen<FEPackagerMenu> {
    public FEPackagerScreen(FEPackagerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void drawMachine(GuiGraphics g, int x, int y) {
        MachineScreenHelper.drawArrow(g, x + 92, y + 35, menu.getProgress(), Math.max(menu.getMaxProgress(), 1), 14);

        int needed = Math.max(1, SkofcraftConfig.boxCapacity);
        int stored = menu.getBlockEntity().getInputSlots().getItem(0).getCount();
        String counter = stored + "/" + needed;
        int color = stored >= needed ? MachineScreenHelper.COLOR_PROGRESS : MachineScreenHelper.COLOR_TEXT_DIM;
        g.drawString(this.font, counter, x + 44 + 9 - this.font.width(counter) / 2, y + 25, color, false);
    }

    @Override
    protected boolean usesEnergy() {
        return true;
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
