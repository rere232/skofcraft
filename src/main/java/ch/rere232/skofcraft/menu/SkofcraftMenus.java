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

    public static final RegistryObject<MenuType<FEGrinderMenu>> FE_GRINDER = MENUS.register("fe_grinder",
            () -> IForgeMenuType.create((windowId, playerInventory, packetBuffer) -> new FEGrinderMenu(windowId, playerInventory, packetBuffer)));

    public static final RegistryObject<MenuType<FEMixerMenu>> FE_MIXER = MENUS.register("fe_mixer",
            () -> IForgeMenuType.create((windowId, playerInventory, packetBuffer) -> new FEMixerMenu(windowId, playerInventory, packetBuffer)));

    public static final RegistryObject<MenuType<FEPressMenu>> FE_PRESS = MENUS.register("fe_press",
            () -> IForgeMenuType.create((windowId, playerInventory, packetBuffer) -> new FEPressMenu(windowId, playerInventory, packetBuffer)));

    public static final RegistryObject<MenuType<FEPackagerMenu>> FE_PACKAGER = MENUS.register("fe_packager",
            () -> IForgeMenuType.create((windowId, playerInventory, packetBuffer) -> new FEPackagerMenu(windowId, playerInventory, packetBuffer)));
}
