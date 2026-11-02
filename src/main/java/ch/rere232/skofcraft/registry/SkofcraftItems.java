package ch.rere232.skofcraft.registry;

import ch.rere232.skofcraft.entity.Leopard1Tank;
import ch.rere232.skofcraft.entity.MausTank;
import ch.rere232.skofcraft.entity.Tiger2Tank;
import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.config.SkofcraftConfig;
import ch.rere232.skofcraft.item.GumConsumableItem;
import ch.rere232.skofcraft.item.AddictolItem;
import ch.rere232.skofcraft.item.AircraftCarrierItem;
import ch.rere232.skofcraft.item.ApacheItem;
import ch.rere232.skofcraft.item.PostbusItem;
import ch.rere232.skofcraft.item.ItemScannerItem;
import ch.rere232.skofcraft.item.TankItem;
import ch.rere232.skofcraft.item.DamageControlKitItem;
import ch.rere232.skofcraft.item.ClothingItem;
import ch.rere232.skofcraft.item.EnergyDrinkItem;
import ch.rere232.skofcraft.item.OneMillionBeerItem;
import ch.rere232.skofcraft.item.EstrogenPillItem;
import ch.rere232.skofcraft.item.MaskItem;
import ch.rere232.skofcraft.item.OutfitBoxItem;
import ch.rere232.skofcraft.item.TacticalArmorItem;
import ch.rere232.skofcraft.item.TacticalGlovesItem;
import net.minecraft.world.item.ArmorItem;
import ch.rere232.skofcraft.item.GigaIcbmItem;
import ch.rere232.skofcraft.item.OrbitalDesignatorItem;
import ch.rere232.skofcraft.item.SatelliteItem;
import ch.rere232.skofcraft.space.SatelliteType;
import ch.rere232.skofcraft.item.BlahajItem;
import ch.rere232.skofcraft.item.F14Item;
import ch.rere232.skofcraft.item.ChichaPipeItem;
import ch.rere232.skofcraft.item.PouchMixItem;
import ch.rere232.skofcraft.item.PhoneItem;
import ch.rere232.skofcraft.item.SnuffItem;
import ch.rere232.skofcraft.item.VapeItem;
import ch.rere232.skofcraft.item.QuestBookItem;
import ch.rere232.skofcraft.item.SkofcraftBoxItem;
import ch.rere232.skofcraft.item.SkofeeItem;
import ch.rere232.skofcraft.item.SmokeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class SkofcraftItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SkofcraftMod.MODID);

        public static final RegistryObject<Item> TOBACCO_SEEDS = ITEMS.register("tobacco_seeds",
            () -> new ItemNameBlockItem(SkofcraftBlocks.TOBACCO_PLANT.get(), new Item.Properties()));
    public static final RegistryObject<Item> QUEST_BOOK = ITEMS.register("quest_book",
            () -> new QuestBookItem(new Item.Properties()));

    public static final RegistryObject<Item> TOBACCO_LEAF_FRESH = item("tobacco_leaf_fresh");
    public static final RegistryObject<Item> TOBACCO_LEAF_DRY = item("tobacco_leaf_dry");
    public static final RegistryObject<Item> TOBACCO_DUST = item("tobacco_dust");

    public static final RegistryObject<Item> SKOFEE_SEEDS = ITEMS.register("skofee_seeds",
            () -> new ItemNameBlockItem(SkofcraftBlocks.SKOFEE_PLANT.get(), new Item.Properties()));
    public static final RegistryObject<Item> SKOFEE_BEANS_FRESH = item("skofee_beans_fresh");
    public static final RegistryObject<Item> SKOFEE_BEANS_DRY = item("skofee_beans_dry");
    public static final RegistryObject<Item> SKOFEE_BEANS_ROASTED = item("skofee_beans_roasted");
    public static final RegistryObject<Item> SKOFEE_GROUND = item("skofee_ground");
    public static final RegistryObject<Item> SKOFEE_CUP = ITEMS.register("skofee_cup",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> SKOFEE = ITEMS.register("skofee",
            () -> new SkofeeItem(new Item.Properties().stacksTo(16), 0, 0.0F, 1.0F));
    public static final RegistryObject<Item> SKOFEE_LATTE = ITEMS.register("skofee_latte",
            () -> new SkofeeItem(new Item.Properties().stacksTo(16), 4, 0.6F, 0.75F));
    public static final RegistryObject<Item> EMPTY_CAN = item("empty_can");
    public static final RegistryObject<Item> WHITE_MONSTER = ITEMS.register("white_monster",
            () -> new EnergyDrinkItem(new Item.Properties().stacksTo(16), EnergyDrinkItem.Kind.WHITE_MONSTER));
    public static final RegistryObject<Item> ONE_MILLION_BEER = ITEMS.register("one_million_beer",
            () -> new OneMillionBeerItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> RED_BULL = ITEMS.register("red_bull",
            () -> new EnergyDrinkItem(new Item.Properties().stacksTo(16), EnergyDrinkItem.Kind.RED_BULL));

    public static final RegistryObject<Item> VEGETAL_FIBER = item("vegetal_fiber");
    public static final RegistryObject<Item> NEUTRAL_BASE = item("neutral_base");
    public static final RegistryObject<Item> NICOTINE_EXTRACT = item("nicotine_extract");
    public static final RegistryObject<Item> NICOTINE_CRYSTALS = item("nicotine_crystals");
    public static final RegistryObject<Item> NICOTINE_MIX = ITEMS.register("nicotine_mix",
            () -> new PouchMixItem(new Item.Properties()));
    public static final RegistryObject<Item> SNUFF = ITEMS.register("snuff",
            () -> new SnuffItem(new Item.Properties()));
    public static final RegistryObject<Item> EMPTY_VAPE = ITEMS.register("empty_vape",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> VAPE = ITEMS.register("vape",
            () -> new VapeItem(new Item.Properties()));

    public static final RegistryObject<Item> CIGARETTE = ITEMS.register("cigarette",
            () -> new SmokeItem(new Item.Properties(), SmokeItem.CIGARETTE));
    public static final RegistryObject<Item> CIGARILLO = ITEMS.register("cigarillo",
            () -> new SmokeItem(new Item.Properties(), SmokeItem.CIGARILLO));
    public static final RegistryObject<Item> CIGAR = ITEMS.register("cigar",
            () -> new SmokeItem(new Item.Properties().stacksTo(16), SmokeItem.CIGAR));
    public static final RegistryObject<Item> EMPTY_CIGARETTE_PACK = ITEMS.register("empty_cigarette_pack",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> CIGARETTE_PACK = ITEMS.register("cigarette_pack",
            () -> new SkofcraftBoxItem(new Item.Properties(), CIGARETTE::get, EMPTY_CIGARETTE_PACK::get));

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
    public static final RegistryObject<Item> SPIT = ITEMS.register("spit", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PHONE = ITEMS.register("phone",
            () -> new PhoneItem(new Item.Properties()));
    public static final RegistryObject<Item> APACHE = ITEMS.register("apache",
            () -> new ApacheItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> F14 = ITEMS.register("f14",
            () -> new F14Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> MAUS = ITEMS.register("maus",
            () -> new TankItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), SkofcraftEntities.MAUS, "tooltip.skofcraft.maus", MausTank.SPEC.maxFuel()));
    public static final RegistryObject<Item> LEOPARD_1 = ITEMS.register("leopard_1",
            () -> new TankItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), SkofcraftEntities.LEOPARD_1, "tooltip.skofcraft.leopard_1", Leopard1Tank.SPEC.maxFuel()));
    public static final RegistryObject<Item> TIGER_2 = ITEMS.register("tiger_2",
            () -> new TankItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), SkofcraftEntities.TIGER_2, "tooltip.skofcraft.tiger_2", Tiger2Tank.SPEC.maxFuel()));
    public static final RegistryObject<Item> POSTBUS = ITEMS.register("postbus",
            () -> new PostbusItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> ITEM_SCANNER = ITEMS.register("item_scanner",
            () -> new ItemScannerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> HR_CERTIFICATE = item("hr_certificate");

    public static final RegistryObject<Item> GEAR = item("gear");
    public static final RegistryObject<Item> AIRCRAFT_PLATE = item("aircraft_plate");
    public static final RegistryObject<Item> TURBINE_BLADE = item("turbine_blade");
    public static final RegistryObject<Item> ROTOR_BLADE = ITEMS.register("rotor_blade",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> AVIONICS = ITEMS.register("avionics",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> JET_ENGINE = ITEMS.register("jet_engine",
            () -> new Item(new Item.Properties().stacksTo(4).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> WEAPON_SYSTEM = ITEMS.register("weapon_system",
            () -> new Item(new Item.Properties().stacksTo(4).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> EMPTY_FUEL_CANISTER = ITEMS.register("empty_fuel_canister",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> JET_FUEL_CANISTER = ITEMS.register("jet_fuel_canister",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> DAMAGE_CONTROL_KIT = ITEMS.register("damage_control_kit",
            () -> new DamageControlKitItem(new Item.Properties().durability(512)));
    public static final RegistryObject<Item> RECON_SATELLITE = ITEMS.register("recon_satellite",
            () -> new SatelliteItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), SatelliteType.RECON));
    public static final RegistryObject<Item> STRIKE_SATELLITE = ITEMS.register("strike_satellite",
            () -> new SatelliteItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), SatelliteType.STRIKE));
    public static final RegistryObject<Item> ORBITAL_DESIGNATOR = ITEMS.register("orbital_designator",
            () -> new OrbitalDesignatorItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> DRACONIC_WARHEAD = ITEMS.register("draconic_warhead",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> ICBM_GUIDANCE = ITEMS.register("icbm_guidance",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> ICBM_BOOSTER = ITEMS.register("icbm_booster",
            () -> new Item(new Item.Properties().stacksTo(4).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> GIGA_ICBM = ITEMS.register("giga_icbm",
            () -> new GigaIcbmItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), 1.0F));
    /** Creative only: five times the Giga ICBM in every direction. */
    public static final RegistryObject<Item> DOOMSDAY_MISSILE = ITEMS.register("doomsday_missile",
            () -> new GigaIcbmItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), 5.0F));
    public static final RegistryObject<Item> COMPOSITE_PLATE = ITEMS.register("composite_plate",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON).fireResistant()));
    public static final RegistryObject<Item> TACTICAL_CORE = ITEMS.register("tactical_core",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> GAS_FILTER = ITEMS.register("gas_filter",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> TACTICAL_HELMET = tacticalArmor("tactical_helmet", ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> TACTICAL_CHESTPLATE = tacticalArmor("tactical_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> TACTICAL_LEGGINGS = tacticalArmor("tactical_leggings", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> TACTICAL_BOOTS = tacticalArmor("tactical_boots", ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> TACTICAL_GLOVES = ITEMS.register("tactical_gloves",
            () -> new TacticalGlovesItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> AIRCRAFT_CARRIER = ITEMS.register("aircraft_carrier",
            () -> new AircraftCarrierItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> ADDICTOL = ITEMS.register("addictol",
            () -> new AddictolItem(new Item.Properties().stacksTo(4).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> CHICHA_PIPE = ITEMS.register("chicha_pipe",
            () -> new ChichaPipeItem(new Item.Properties()));
    public static final RegistryObject<Item> CHEWING_TOBACCO = ITEMS.register("chewing_tobacco",
            () -> new GumConsumableItem(new Item.Properties().stacksTo(16), () -> SkofcraftConfig.chewDurationMinutes));

    public static final RegistryObject<Item> EMPTY_SNUS_BOX = item("empty_snus_box");
    public static final RegistryObject<Item> FILLED_SNUS_BOX = ITEMS.register("filled_snus_box",
            () -> new SkofcraftBoxItem(new Item.Properties(), SNUS_POUCH::get, EMPTY_SNUS_BOX::get));

    public static final RegistryObject<Item> EMPTY_POUCH_BOX = item("empty_pouch_box");
    public static final RegistryObject<Item> FILLED_POUCH_BOX = ITEMS.register("filled_pouch_box",
            () -> new SkofcraftBoxItem(new Item.Properties(), NICOTINE_POUCH::get, EMPTY_POUCH_BOX::get));

    public static final RegistryObject<Item> TOBACCO_PLANT = blockItem("tobacco_plant", SkofcraftBlocks.TOBACCO_PLANT);
    public static final RegistryObject<Item> CHOKER = ITEMS.register("choker",
            () -> new ClothingItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SKIRT = ITEMS.register("skirt",
            () -> new ClothingItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> THIGH_HIGHS = ITEMS.register("thigh_highs",
            () -> new ClothingItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CROP_TOP = ITEMS.register("crop_top",
            () -> new ClothingItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> HAND_WARMERS = ITEMS.register("hand_warmers",
            () -> new ClothingItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FACE_MASK = ITEMS.register("face_mask",
            () -> new MaskItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CAT_EARS = ITEMS.register("cat_ears",
            () -> new ClothingItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CAT_TAIL = ITEMS.register("cat_tail",
            () -> new ClothingItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> OUTFIT_BOX = ITEMS.register("outfit_box",
            () -> new OutfitBoxItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> ESTROGEN_PILL = ITEMS.register("estrogen_pill",
            () -> new EstrogenPillItem(new Item.Properties()));
    public static final RegistryObject<Item> FEMBOY_SPAWN_EGG = ITEMS.register("femboy_spawn_egg",
            () -> new ForgeSpawnEggItem(SkofcraftEntities.FEMBOY, 0xF5A9B8, 0x1C1C1C, new Item.Properties()));
    public static final RegistryObject<Item> ANIME_CREW_SPAWN_EGG = ITEMS.register("anime_crew_spawn_egg",
            () -> new ForgeSpawnEggItem(SkofcraftEntities.ANIME_CREW, 0x5B6B3A, 0xF08CB4, new Item.Properties()));
    public static final RegistryObject<Item> BLAHAJ = ITEMS.register("blahaj",
            () -> new BlahajItem(SkofcraftBlocks.BLAHAJ.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CHICHA = blockItem("chicha", SkofcraftBlocks.CHICHA);
    public static final RegistryObject<Item> BIG_RED_BUTTON = blockItem("big_red_button", SkofcraftBlocks.BIG_RED_BUTTON);
    public static final RegistryObject<Item> EVIL_PRESSURE_PLATE = blockItem("evil_pressure_plate", SkofcraftBlocks.EVIL_PRESSURE_PLATE);
    public static final RegistryObject<Item> EVIL_TRIPWIRE = blockItem("evil_tripwire", SkofcraftBlocks.EVIL_TRIPWIRE);
    public static final RegistryObject<Item> EVIL_LEVER = blockItem("evil_lever", SkofcraftBlocks.EVIL_LEVER);
    public static final RegistryObject<Item> EXCALIBUR = ITEMS.register("excalibur",
            () -> new BlockItem(SkofcraftBlocks.EXCALIBUR.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> SKOFEE_MACHINE = blockItem("skofee_machine", SkofcraftBlocks.SKOFEE_MACHINE);
    public static final RegistryObject<Item> AIRCRAFT_WORKBENCH = blockItem("aircraft_workbench", SkofcraftBlocks.AIRCRAFT_WORKBENCH);
    public static final RegistryObject<Item> CARRIER_HELM = blockItem("carrier_helm", SkofcraftBlocks.CARRIER_HELM);
    public static final RegistryObject<Item> RADAR = blockItem("radar", SkofcraftBlocks.RADAR);
    public static final RegistryObject<Item> CIWS = blockItem("ciws", SkofcraftBlocks.CIWS);
    public static final RegistryObject<Item> SAM_LAUNCHER = blockItem("sam_launcher", SkofcraftBlocks.SAM_LAUNCHER);
    public static final RegistryObject<Item> LAUNCH_PAD = blockItem("launch_pad", SkofcraftBlocks.LAUNCH_PAD);
    public static final RegistryObject<Item> CATAPULT = blockItem("catapult", SkofcraftBlocks.CATAPULT);
    public static final RegistryObject<Item> ARRESTING_WIRE = blockItem("arresting_wire", SkofcraftBlocks.ARRESTING_WIRE);
    public static final RegistryObject<Item> NUCLEAR_REACTOR = ITEMS.register("nuclear_reactor",
            () -> new BlockItem(SkofcraftBlocks.NUCLEAR_REACTOR.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> TOBACCONIST_COUNTER = blockItem("tobacconist_counter", SkofcraftBlocks.TOBACCONIST_COUNTER);
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
        return ITEMS.register(name, () -> new GumConsumableItem(new Item.Properties().stacksTo(1), durationSupplier));
    }

    private static RegistryObject<Item> tacticalArmor(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new TacticalArmorItem(type, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    }

    private static RegistryObject<Item> blockItem(String name, Supplier<? extends net.minecraft.world.level.block.Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
