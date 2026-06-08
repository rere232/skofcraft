package ch.rere232.skofcraft.menu;

import ch.rere232.skofcraft.SkofcraftMod;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.network.IContainerFactory;

public class SkofcraftMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, SkofcraftMod.MODID);

    public static final RegistryObject<MenuType<FEDryerMenu>> FE_DRYER = MENUS.register("fe_dryer",
            () -> IForgeMenuType.create((windowId, playerInventory, packetBuffer) -> new FEDryerMenu(windowId, playerInventory, packetBuffer)));
}
