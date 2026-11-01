package ch.rere232.skofcraft.screen;

import ch.rere232.skofcraft.menu.AircraftWorkbenchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AircraftWorkbenchScreen extends AbstractMachineScreen<AircraftWorkbenchMenu> {

    public AircraftWorkbenchScreen(AircraftWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = AircraftWorkbenchMenu.INVENTORY_Y + 82;
        this.inventoryLabelY = AircraftWorkbenchMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void drawMachine(GuiGraphics g, int x, int y) {
        int arrowX = AircraftWorkbenchMenu.GRID_X + AircraftWorkbenchMenu.GRID_WIDTH * 18 + 6;
        boolean ready = menu.getSlot(AircraftWorkbenchMenu.GRID_WIDTH * AircraftWorkbenchMenu.GRID_HEIGHT).hasItem();
        MachineScreenHelper.drawArrow(g, x + arrowX, y + AircraftWorkbenchMenu.RESULT_Y, ready ? 1 : 0, 1, 18);
    }
}
