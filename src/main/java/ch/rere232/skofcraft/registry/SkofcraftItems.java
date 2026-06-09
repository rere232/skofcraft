package ch.rere232.skofcraft.registry;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.config.SkofcraftConfig;
import ch.rere232.skofcraft.item.GumConsumableItem;
import ch.rere232.skofcraft.item.SkofcraftBoxItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class SkofcraftItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SkofcraftMod.MODID);

    public static final RegistryObject<Item> TOBACCO_SEEDS = item("tobacco_seeds");
    public static final RegistryObject<Item> TOBACCO_LEAF_FRESH = item("tobacco_leaf_fresh");
    public static final RegistryObject<Item> TOBACCO_LEAF_DRY = item("tobacco_leaf_dry");
    public static final RegistryObject<Item> TOBACCO_DUST = item("tobacco_dust");

    public static final RegistryObject<Item> VEGETAL_FIBER = item("vegetal_fiber");
    public static final RegistryObject<Item> NEUTRAL_BASE = item("neutral_base");
    public static final RegistryObject<Item> NICOTINE_EXTRACT = item("nicotine_extract");
    public static final RegistryObject<Item> NICOTINE_CRYSTALS = item("nicotine_crystals");

    public static final RegistryObject<Item> FLAVOR_MINT = item("flavor_mint");
    public static final RegistryObject<Item> FLAVOR_ICE_MINT = item("flavor_ice_mint");
    public static final RegistryObject<Item> FLAVOR_BERRY = item("flavor_berry");
    public static final RegistryObject<Item> FLAVOR_CITRUS = item("flavor_citrus");
    public static final RegistryObject<Item> FLAVOR_LICORICE = item("flavor_licorice");
    public static final RegistryObject<Item> FLAVOR_COLA = item("flavor_cola");
    public static final RegistryObject<Item> FLAVOR_COFFEE = item("flavor_coffee");
    public static final RegistryObject<Item> FLAVOR_NATURE = item("flavor_nature");

    public static final RegistryObject<Item> EMPTY_POUCH = item("empty_pouch");
    public static final RegistryObject<Item> SNUS_POUCH = gumItem("snus_pouch", () -> SkofcraftConfig.snusDurationMinutes);
    public static final RegistryObject<Item> NICOTINE_POUCH = gumItem("nicotine_pouch", () -> SkofcraftConfig.pouchDurationMinutes);

    public static final RegistryObject<Item> EMPTY_SNUS_BOX = item("empty_snus_box");
    public static final RegistryObject<Item> FILLED_SNUS_BOX = ITEMS.register("filled_snus_box",
            () -> new SkofcraftBoxItem(new Item.Properties(), SNUS_POUCH::get, EMPTY_SNUS_BOX::get));

    public static final RegistryObject<Item> EMPTY_POUCH_BOX = item("empty_pouch_box");
    public static final RegistryObject<Item> FILLED_POUCH_BOX = ITEMS.register("filled_pouch_box",
            () -> new SkofcraftBoxItem(new Item.Properties(), NICOTINE_POUCH::get, EMPTY_POUCH_BOX::get));

    public static final RegistryObject<Item> TOBACCO_PLANT = blockItem("tobacco_plant", SkofcraftBlocks.TOBACCO_PLANT);
    public static final RegistryObject<Item> MANUAL_DRYER = blockItem("manual_dryer", SkofcraftBlocks.MANUAL_DRYER);
    public static final RegistryObject<Item> MANUAL_GRINDER = blockItem("manual_grinder", SkofcraftBlocks.MANUAL_GRINDER);
    public static final RegistryObject<Item> MANUAL_MIXER = blockItem("manual_mixer", SkofcraftBlocks.MANUAL_MIXER);
    public static final RegistryObject<Item> MANUAL_POUCH_PRESS = blockItem("manual_pouch_press", SkofcraftBlocks.MANUAL_POUCH_PRESS);

    public static final RegistryObject<Item> FE_DRYER = blockItem("fe_dryer", SkofcraftBlocks.FE_DRYER);
    public static final RegistryObject<Item> FE_GRINDER = blockItem("fe_grinder", SkofcraftBlocks.FE_GRINDER);
    public static final RegistryObject<Item> FE_MIXER = blockItem("fe_mixer", SkofcraftBlocks.FE_MIXER);
    public static final RegistryObject<Item> FE_PRESS = blockItem("fe_press", SkofcraftBlocks.FE_PRESS);
    public static final RegistryObject<Item> FE_NICOTINE_EXTRACTOR = blockItem("fe_nicotine_extractor", SkofcraftBlocks.FE_NICOTINE_EXTRACTOR);
    public static final RegistryObject<Item> FE_PACKAGER = blockItem("fe_packager", SkofcraftBlocks.FE_PACKAGER);

    public static final RegistryObject<Item> INDUSTRIAL_SNUS_LINE = blockItem("industrial_snus_line", SkofcraftBlocks.INDUSTRIAL_SNUS_LINE);
    public static final RegistryObject<Item> INDUSTRIAL_POUCH_LINE = blockItem("industrial_pouch_line", SkofcraftBlocks.INDUSTRIAL_POUCH_LINE);
    public static final RegistryObject<Item> INDUSTRIAL_PACKAGER = blockItem("industrial_packager", SkofcraftBlocks.INDUSTRIAL_PACKAGER);
    public static final RegistryObject<Item> INDUSTRIAL_EXTRACTOR = blockItem("industrial_extractor", SkofcraftBlocks.INDUSTRIAL_EXTRACTOR);

    private static RegistryObject<Item> item(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static RegistryObject<Item> gumItem(String name, Supplier<Integer> durationSupplier) {
        return ITEMS.register(name, () -> new GumConsumableItem(new Item.Properties().stacksTo(16), durationSupplier));
    }

    private static RegistryObject<Item> blockItem(String name, Supplier<? extends net.minecraft.world.level.block.Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
