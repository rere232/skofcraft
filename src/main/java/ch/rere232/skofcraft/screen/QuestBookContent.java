package ch.rere232.skofcraft.screen;

import ch.rere232.skofcraft.config.SkofcraftConfig;
import ch.rere232.skofcraft.item.GumConsumableItem;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Chapters shown in the quest book. Text lives in the lang files under questbook.skofcraft.*. */
@OnlyIn(Dist.CLIENT)
final class QuestBookContent {
    interface Entry {}

    record Text(Component text) implements Entry {}

    /** Crafting grid; empty cells are ItemStack.EMPTY. */
    record Craft(ItemStack[] grid, int width, int height, ItemStack result) implements Entry {}

    /** Inputs processed by a machine into one output. */
    record Process(List<ItemStack> inputs, ItemStack machine, ItemStack output) implements Entry {}

    record Chapter(Component title, ItemStack icon, List<Entry> entries) {}

    private QuestBookContent() {}

    static List<Chapter> build() {
        List<Chapter> chapters = new ArrayList<>();

        chapters.add(new Chapter(t("intro.title"), stack(SkofcraftItems.FILLED_SNUS_BOX.get()), List.of(
                text("intro.p1"), text("intro.p2"), text("intro.p3"), text("intro.p4")
        )));

        chapters.add(new Chapter(t("farming.title"), stack(SkofcraftItems.TOBACCO_SEEDS.get()), List.of(
                text("farming.p1"),
                craft(stack(SkofcraftItems.TOBACCO_SEEDS.get()), new String[] {"   ", " L ", "   "}, 'L', Items.OAK_LEAVES),
                text("farming.p2"), text("farming.p3"), text("farming.p4")
        )));

        chapters.add(new Chapter(t("drying.title"), stack(SkofcraftItems.MANUAL_DRYER.get()), List.of(
                text("drying.p1"),
                craft(stack(SkofcraftItems.MANUAL_DRYER.get()), new String[] {"WWW", "WSW", "WWW"},
                        'W', Items.DARK_OAK_WOOD, 'S', Items.STICK),
                process(SkofcraftItems.MANUAL_DRYER.get(), stack(SkofcraftItems.TOBACCO_LEAF_DRY.get()),
                        stack(SkofcraftItems.TOBACCO_LEAF_FRESH.get())),
                text("drying.p2")
        )));

        chapters.add(new Chapter(t("grinding.title"), stack(SkofcraftItems.MANUAL_GRINDER.get()), List.of(
                text("grinding.p1"),
                craft(stack(SkofcraftItems.MANUAL_GRINDER.get()), new String[] {"III", "ISI", "III"},
                        'I', Items.IRON_INGOT, 'S', Items.SMOOTH_STONE),
                process(SkofcraftItems.MANUAL_GRINDER.get(), new ItemStack(SkofcraftItems.TOBACCO_DUST.get(), 2),
                        stack(SkofcraftItems.TOBACCO_LEAF_DRY.get())),
                text("grinding.p2")
        )));

        chapters.add(new Chapter(t("snus.title"), stack(SkofcraftItems.MANUAL_POUCH_PRESS.get()), List.of(
                text("snus.p1"),
                craft(stack(SkofcraftItems.MANUAL_POUCH_PRESS.get()), new String[] {"III", "IPI", "III"},
                        'I', Items.IRON_INGOT, 'P', Items.PISTON),
                process(SkofcraftItems.MANUAL_POUCH_PRESS.get(), stack(SkofcraftItems.SNUS_POUCH.get()),
                        stack(SkofcraftItems.TOBACCO_DUST.get()), stack(SkofcraftItems.EMPTY_POUCH.get())),
                text("snus.p2")
        )));

        ItemStack mintMix = stack(SkofcraftItems.NICOTINE_MIX.get());
        GumConsumableItem.setFlavor(mintMix, stack(SkofcraftItems.FLAVOR_MINT.get()));
        ItemStack mintPouch = stack(SkofcraftItems.NICOTINE_POUCH.get());
        GumConsumableItem.setFlavor(mintPouch, stack(SkofcraftItems.FLAVOR_MINT.get()));
        chapters.add(new Chapter(t("nicotine.title"), stack(SkofcraftItems.NICOTINE_EXTRACT.get()), List.of(
                text("nicotine.p1"),
                craft(new ItemStack(SkofcraftItems.VEGETAL_FIBER.get(), 2), new String[] {"   ", "SSS", "   "},
                        'S', Items.SUGAR_CANE),
                craft(new ItemStack(SkofcraftItems.NEUTRAL_BASE.get(), 4), new String[] {"FF ", "FF ", " W "},
                        'F', SkofcraftItems.VEGETAL_FIBER.get(), 'W', Items.WATER_BUCKET),
                text("nicotine.p2"),
                craft(stack(SkofcraftItems.FE_NICOTINE_EXTRACTOR.get()), new String[] {"RDR", "RCR", "RRR"},
                        'R', Items.REDSTONE, 'D', SkofcraftItems.FE_DRYER.get(), 'C', Items.COPPER_INGOT),
                process(SkofcraftItems.FE_NICOTINE_EXTRACTOR.get(), stack(SkofcraftItems.NICOTINE_EXTRACT.get()),
                        stack(SkofcraftItems.TOBACCO_LEAF_DRY.get())),
                text("nicotine.p3"),
                craft(stack(SkofcraftItems.MANUAL_MIXER.get()), new String[] {"III", "ICI", "III"},
                        'I', Items.IRON_INGOT, 'C', Items.CAULDRON),
                process(SkofcraftItems.MANUAL_MIXER.get(), mintMix,
                        stack(SkofcraftItems.NEUTRAL_BASE.get()), stack(SkofcraftItems.NICOTINE_EXTRACT.get()),
                        stack(SkofcraftItems.FLAVOR_MINT.get())),
                process(SkofcraftItems.MANUAL_POUCH_PRESS.get(), mintPouch,
                        mintMix, stack(SkofcraftItems.EMPTY_POUCH.get())),
                text("nicotine.p4"),
                process(Items.FURNACE, stack(SkofcraftItems.NICOTINE_CRYSTALS.get()),
                        stack(SkofcraftItems.NICOTINE_EXTRACT.get())),
                text("nicotine.p5")
        )));

        chapters.add(new Chapter(t("flavors.title"), stack(SkofcraftItems.FLAVOR_BERRY.get()), List.of(
                text("flavors.p1"),
                text("flavors.mint"), text("flavors.ice_mint"), text("flavors.berry"), text("flavors.citrus"),
                text("flavors.cola"), text("flavors.licorice"), text("flavors.coffee"), text("flavors.nature"),
                craft(new ItemStack(SkofcraftItems.FLAVOR_COFFEE.get(), 2), new String[] {"   ", " GS", "   "},
                        'G', SkofcraftItems.SKOFEE_GROUND.get(), 'S', Items.SUGAR),
                text("flavors.p2")
        )));

        int capacity = Math.max(1, SkofcraftConfig.boxCapacity);
        ItemStack mintSnuff = stack(SkofcraftItems.SNUFF.get());
        GumConsumableItem.setFlavor(mintSnuff, stack(SkofcraftItems.FLAVOR_MINT.get()));
        chapters.add(new Chapter(t("snuff.title"), mintSnuff, List.of(
                text("snuff.p1"),
                process(SkofcraftItems.MANUAL_MIXER.get(), mintSnuff,
                        stack(SkofcraftItems.TOBACCO_DUST.get()), ItemStack.EMPTY, stack(SkofcraftItems.FLAVOR_MINT.get())),
                text("snuff.p2"), text("snuff.p3")
        )));

        chapters.add(new Chapter(t("cigar.title"), stack(SkofcraftItems.CIGAR.get()), List.of(
                text("cigar.p1"),
                craft(new ItemStack(SkofcraftItems.CIGARILLO.get(), 2), new String[] {" L ", " D ", " L "},
                        'L', SkofcraftItems.TOBACCO_LEAF_DRY.get(), 'D', SkofcraftItems.TOBACCO_DUST.get()),
                text("cigar.p2"),
                craft(stack(SkofcraftItems.CIGAR.get()), new String[] {"LLL", "DDD", " G "},
                        'L', SkofcraftItems.TOBACCO_LEAF_DRY.get(), 'D', SkofcraftItems.TOBACCO_DUST.get(), 'G', Items.GOLD_NUGGET),
                text("cigar.p3")
        )));

        chapters.add(new Chapter(t("chew.title"), stack(SkofcraftItems.CHEWING_TOBACCO.get()), List.of(
                text("chew.p1"),
                craft(new ItemStack(SkofcraftItems.CHEWING_TOBACCO.get(), 2), new String[] {"LL ", "LS ", "   "},
                        'L', SkofcraftItems.TOBACCO_LEAF_DRY.get(), 'S', Items.SUGAR),
                text("chew.p2"), text("chew.p3"), text("chew.p4")
        )));

        chapters.add(new Chapter(t("chicha.title"), stack(SkofcraftItems.CHICHA.get()), List.of(
                text("chicha.p1"),
                craft(stack(SkofcraftItems.CHICHA.get()), new String[] {"SBS", "SGS", "III"},
                        'S', Items.STRING, 'B', Items.BOWL, 'G', Items.GLASS_BOTTLE, 'I', Items.IRON_INGOT),
                text("chicha.p2"), text("chicha.p3"), text("chicha.p4")
        )));

        ItemStack mintVape = stack(SkofcraftItems.VAPE.get());
        GumConsumableItem.setFlavor(mintVape, stack(SkofcraftItems.FLAVOR_MINT.get()));
        chapters.add(new Chapter(t("vape.title"), stack(SkofcraftItems.VAPE.get()), List.of(
                text("vape.p1"),
                craft(stack(SkofcraftItems.EMPTY_VAPE.get()), new String[] {" N ", " G ", " C "},
                        'N', Items.IRON_NUGGET, 'G', Items.GLASS_PANE, 'C', Items.COPPER_INGOT),
                text("vape.p2"),
                process(SkofcraftItems.MANUAL_MIXER.get(), mintVape,
                        stack(SkofcraftItems.EMPTY_VAPE.get()), mintMix),
                text("vape.p3")
        )));

        chapters.add(new Chapter(t("cigarette.title"), stack(SkofcraftItems.CIGARETTE.get()), List.of(
                text("cigarette.p1"),
                craft(new ItemStack(SkofcraftItems.CIGARETTE.get(), 2), new String[] {" F ", " D ", " P "},
                        'F', SkofcraftItems.VEGETAL_FIBER.get(), 'D', SkofcraftItems.TOBACCO_DUST.get(), 'P', Items.PAPER),
                text("cigarette.p2"),
                text("cigarette.p3"),
                craft(stack(SkofcraftItems.EMPTY_CIGARETTE_PACK.get()), new String[] {"PP ", "PP ", "   "},
                        'P', Items.PAPER),
                process(SkofcraftItems.FE_PACKAGER.get(), stack(SkofcraftItems.CIGARETTE_PACK.get()),
                        new ItemStack(SkofcraftItems.CIGARETTE.get(), capacity),
                        stack(SkofcraftItems.EMPTY_CIGARETTE_PACK.get())),
                text("cigarette.p4")
        )));

        chapters.add(new Chapter(t("packaging.title"), stack(SkofcraftItems.FILLED_SNUS_BOX.get()), List.of(
                text("packaging.p1"),
                craft(stack(SkofcraftItems.EMPTY_SNUS_BOX.get()), new String[] {"PPP", "PCP", "PPP"},
                        'P', Items.DARK_OAK_PLANKS, 'C', Items.CHEST),
                craft(stack(SkofcraftItems.EMPTY_POUCH_BOX.get()), new String[] {"PPP", "PCP", "PPP"},
                        'P', Items.PAPER, 'C', Items.CHEST),
                text("packaging.p2"),
                process(SkofcraftItems.FE_PACKAGER.get(), stack(SkofcraftItems.FILLED_SNUS_BOX.get()),
                        new ItemStack(SkofcraftItems.SNUS_POUCH.get(), capacity),
                        stack(SkofcraftItems.EMPTY_SNUS_BOX.get())),
                text("packaging.p3")
        )));

        chapters.add(new Chapter(t("power.title"), stack(SkofcraftItems.FE_DRYER.get()), List.of(
                text("power.p1"),
                feCraft(SkofcraftItems.FE_DRYER.get(), SkofcraftItems.MANUAL_DRYER.get()),
                feCraft(SkofcraftItems.FE_GRINDER.get(), SkofcraftItems.MANUAL_GRINDER.get()),
                feCraft(SkofcraftItems.FE_MIXER.get(), SkofcraftItems.MANUAL_MIXER.get()),
                feCraft(SkofcraftItems.FE_PRESS.get(), Items.DISPENSER),
                feCraft(SkofcraftItems.FE_PACKAGER.get(), SkofcraftItems.FE_PRESS.get()),
                text("power.p2")
        )));

        chapters.add(new Chapter(t("industrial.title"), stack(SkofcraftItems.INDUSTRIAL_PACKAGER.get()), List.of(
                text("industrial.p1"),
                craft(stack(SkofcraftItems.INDUSTRIAL_EXTRACTOR.get()), new String[] {"RER", "RIR", "RRR"},
                        'R', Items.REDSTONE_BLOCK, 'E', SkofcraftItems.FE_NICOTINE_EXTRACTOR.get(), 'I', Items.IRON_BLOCK),
                craft(stack(SkofcraftItems.INDUSTRIAL_PACKAGER.get()), new String[] {"RPR", "RIR", "RRR"},
                        'R', Items.REDSTONE_BLOCK, 'P', SkofcraftItems.FE_PACKAGER.get(), 'I', Items.IRON_BLOCK),
                craft(stack(SkofcraftItems.INDUSTRIAL_POUCH_LINE.get()), new String[] {"RPR", "RMR", "RRR"},
                        'R', Items.REDSTONE, 'P', SkofcraftItems.FE_PRESS.get(), 'M', SkofcraftItems.FE_MIXER.get()),
                craft(stack(SkofcraftItems.INDUSTRIAL_SNUS_LINE.get()), new String[] {"RMR", "RER", "RRR"},
                        'R', Items.REDSTONE, 'M', SkofcraftItems.FE_MIXER.get(), 'E', SkofcraftItems.FE_NICOTINE_EXTRACTOR.get()),
                text("industrial.p2")
        )));

        chapters.add(new Chapter(t("skofee.title"), stack(SkofcraftItems.SKOFEE.get()), List.of(
                text("skofee.p1"),
                craft(new ItemStack(SkofcraftItems.SKOFEE_SEEDS.get(), 2), new String[] {"   ", " CW", "   "},
                        'C', Items.COCOA_BEANS, 'W', Items.WHEAT_SEEDS),
                text("skofee.p2"),
                process(SkofcraftItems.MANUAL_DRYER.get(), stack(SkofcraftItems.SKOFEE_BEANS_DRY.get()),
                        stack(SkofcraftItems.SKOFEE_BEANS_FRESH.get())),
                text("skofee.p3"),
                process(Items.FURNACE, stack(SkofcraftItems.SKOFEE_BEANS_ROASTED.get()),
                        stack(SkofcraftItems.SKOFEE_BEANS_DRY.get())),
                process(SkofcraftItems.MANUAL_GRINDER.get(), new ItemStack(SkofcraftItems.SKOFEE_GROUND.get(), 2),
                        stack(SkofcraftItems.SKOFEE_BEANS_ROASTED.get())),
                text("skofee.p4"),
                craft(new ItemStack(SkofcraftItems.SKOFEE_CUP.get(), 4), new String[] {"B B", " B ", "   "},
                        'B', Items.BRICK),
                craft(stack(SkofcraftItems.SKOFEE_MACHINE.get()), new String[] {"IBI", "GFG", "III"},
                        'I', Items.IRON_INGOT, 'B', Items.BUCKET, 'G', Items.GLASS_PANE, 'F', Items.FURNACE),
                text("skofee.p5"),
                process(SkofcraftItems.SKOFEE_MACHINE.get(), stack(SkofcraftItems.SKOFEE.get()),
                        stack(SkofcraftItems.SKOFEE_CUP.get()), stack(SkofcraftItems.SKOFEE_GROUND.get()),
                        stack(Items.WATER_BUCKET)),
                text("skofee.p6"),
                craft(stack(SkofcraftItems.SKOFEE_LATTE.get()), new String[] {"   ", " SM", "   "},
                        'S', SkofcraftItems.SKOFEE.get(), 'M', Items.MILK_BUCKET),
                craft(new ItemStack(SkofcraftItems.FLAVOR_COFFEE.get(), 2), new String[] {"   ", " GS", "   "},
                        'G', SkofcraftItems.SKOFEE_GROUND.get(), 'S', Items.SUGAR),
                text("skofee.p7"),
                text("skofee.cans"),
                craft(stack(SkofcraftItems.EMPTY_CAN.get()), new String[] {"N N", " N "}, 'N', Items.IRON_NUGGET),
                text("skofee.white_monster"),
                craft(stack(SkofcraftItems.WHITE_MONSTER.get()), new String[] {"SBW", " C "},
                        'S', Items.SUGAR, 'B', SkofcraftItems.SKOFEE_BEANS_ROASTED.get(), 'W', Items.WHITE_DYE, 'C', SkofcraftItems.EMPTY_CAN.get()),
                text("skofee.red_bull"),
                craft(stack(SkofcraftItems.RED_BULL.get()), new String[] {"SBF", " C "},
                        'S', Items.SUGAR, 'B', SkofcraftItems.SKOFEE_BEANS_ROASTED.get(), 'F', Items.FEATHER, 'C', SkofcraftItems.EMPTY_CAN.get())
        )));

        chapters.add(new Chapter(t("phone.title"), stack(SkofcraftItems.PHONE.get()), List.of(
                text("phone.p1"),
                craft(stack(SkofcraftItems.PHONE.get()), new String[] {"NNN", "NGN", "CRC"},
                        'N', Items.IRON_NUGGET, 'G', Items.GLASS_PANE, 'C', Items.COPPER_INGOT, 'R', Items.REDSTONE),
                text("phone.p2"), text("phone.p3"), text("phone.p4")
        )));

        chapters.add(new Chapter(t("aircraft.title"), stack(SkofcraftItems.AIRCRAFT_WORKBENCH.get()), List.of(
                text("aircraft.p1"),
                craft(stack(SkofcraftItems.AIRCRAFT_WORKBENCH.get()), new String[] {"PPP", "GTG", "IBI"},
                        'P', SkofcraftItems.AIRCRAFT_PLATE.get(), 'G', SkofcraftItems.GEAR.get(), 'T', Items.CRAFTING_TABLE,
                        'I', Items.IRON_INGOT, 'B', Items.IRON_BLOCK),
                text("aircraft.p2"),
                craft(new ItemStack(SkofcraftItems.GEAR.get(), 2), new String[] {" N ", "NIN", " N "},
                        'N', Items.IRON_NUGGET, 'I', Items.IRON_INGOT),
                craft(new ItemStack(SkofcraftItems.AIRCRAFT_PLATE.get(), 2), new String[] {"ICI"},
                        'I', Items.IRON_INGOT, 'C', Items.COPPER_INGOT),
                craft(new ItemStack(SkofcraftItems.TURBINE_BLADE.get(), 2), new String[] {"  P", " P ", "G  "},
                        'P', SkofcraftItems.AIRCRAFT_PLATE.get(), 'G', Items.GOLD_INGOT),
                craft(stack(SkofcraftItems.ROTOR_BLADE.get()), new String[] {"PPP", " I "},
                        'P', SkofcraftItems.AIRCRAFT_PLATE.get(), 'I', Items.IRON_INGOT),
                craft(stack(SkofcraftItems.AVIONICS.get()), new String[] {"QRQ", "COC", "GRG"},
                        'Q', Items.QUARTZ, 'R', Items.REDSTONE, 'C', Items.COMPARATOR, 'O', Items.OBSERVER, 'G', Items.GOLD_INGOT),
                text("aircraft.p3"),
                craft(stack(SkofcraftItems.JET_ENGINE.get()), new String[] {"PPPPP", "TGBGT", "TFDFT", "PPPPP"},
                        'P', SkofcraftItems.AIRCRAFT_PLATE.get(), 'T', SkofcraftItems.TURBINE_BLADE.get(), 'G', SkofcraftItems.GEAR.get(),
                        'B', Items.BLAZE_ROD, 'F', Items.FIRE_CHARGE, 'D', Items.DIAMOND),
                craft(stack(SkofcraftItems.WEAPON_SYSTEM.get()), new String[] {"PPPPP", "DGTGD", "PRRRP"},
                        'P', SkofcraftItems.AIRCRAFT_PLATE.get(), 'D', Items.DISPENSER, 'G', SkofcraftItems.GEAR.get(),
                        'T', Items.TNT, 'R', Items.REDSTONE_BLOCK),
                text("aircraft.p4"),
                craft(new ItemStack(SkofcraftItems.EMPTY_FUEL_CANISTER.get(), 2), new String[] {" C ", "I I", "III"},
                        'C', Items.COPPER_INGOT, 'I', Items.IRON_INGOT),
                craft(stack(SkofcraftItems.JET_FUEL_CANISTER.get()), new String[] {"EL", "BC"},
                        'E', SkofcraftItems.EMPTY_FUEL_CANISTER.get(), 'L', Items.LAVA_BUCKET, 'B', Items.BLAZE_POWDER, 'C', Items.COAL),
                text("aircraft.p5")
        )));

        chapters.add(new Chapter(t("apache.title"), stack(SkofcraftItems.APACHE.get()), List.of(
                text("apache.p1"),
                craft(stack(SkofcraftItems.APACHE.get()), new String[] {"RRRRR", "GPAPG", "WPJPW", "PCBCP"},
                        'R', SkofcraftItems.ROTOR_BLADE.get(), 'G', SkofcraftItems.GEAR.get(), 'P', SkofcraftItems.AIRCRAFT_PLATE.get(),
                        'A', SkofcraftItems.AVIONICS.get(), 'W', SkofcraftItems.WEAPON_SYSTEM.get(), 'J', SkofcraftItems.JET_ENGINE.get(),
                        'C', Items.GLASS, 'B', Items.IRON_BLOCK),
                text("apache.p2"), text("apache.p3")
        )));

        chapters.add(new Chapter(t("f14.title"), stack(SkofcraftItems.F14.get()), List.of(
                text("f14.p1"),
                craft(stack(SkofcraftItems.F14.get()), new String[] {"PPCPP", "WPAPW", "PJGJP", "GPBPG"},
                        'P', SkofcraftItems.AIRCRAFT_PLATE.get(), 'C', Items.GLASS, 'W', SkofcraftItems.WEAPON_SYSTEM.get(),
                        'A', SkofcraftItems.AVIONICS.get(), 'J', SkofcraftItems.JET_ENGINE.get(), 'G', SkofcraftItems.GEAR.get(),
                        'B', Items.IRON_BLOCK),
                text("f14.p2"), text("f14.p3")
        )));

        chapters.add(new Chapter(t("carrier.title"), stack(SkofcraftItems.AIRCRAFT_CARRIER.get()), List.of(
                text("carrier.p1"),
                craft(stack(SkofcraftItems.NUCLEAR_REACTOR.get()), new String[] {"IGRGI", "RANAR", "IGRGI"},
                        'I', Items.IRON_BLOCK, 'G', Items.GLOWSTONE, 'R', Items.REDSTONE_BLOCK,
                        'A', SkofcraftItems.AVIONICS.get(), 'N', Items.NETHER_STAR),
                text("carrier.p2"),
                craft(stack(SkofcraftItems.CARRIER_HELM.get()), new String[] {" A ", "PCP", "PPP"},
                        'A', SkofcraftItems.AVIONICS.get(), 'C', Items.COMPASS, 'P', SkofcraftItems.AIRCRAFT_PLATE.get()),
                craft(new ItemStack(SkofcraftItems.CATAPULT.get(), 4), new String[] {"PPP", "GXG", "PPP"},
                        'P', SkofcraftItems.AIRCRAFT_PLATE.get(), 'G', SkofcraftItems.GEAR.get(), 'X', Items.PISTON),
                craft(new ItemStack(SkofcraftItems.ARRESTING_WIRE.get(), 4), new String[] {"CCC", "PPP"},
                        'C', Items.CHAIN, 'P', SkofcraftItems.AIRCRAFT_PLATE.get()),
                text("carrier.p3"),
                craft(stack(SkofcraftItems.AIRCRAFT_CARRIER.get()), new String[] {"BBBBB", "JNHNJ", "WBBBW", "BBBBB"},
                        'B', Items.IRON_BLOCK, 'J', SkofcraftItems.JET_ENGINE.get(), 'N', SkofcraftItems.NUCLEAR_REACTOR.get(),
                        'H', SkofcraftItems.CARRIER_HELM.get(), 'W', SkofcraftItems.WEAPON_SYSTEM.get()),
                text("carrier.p4"), text("carrier.rooms"), text("carrier.p5"),
                text("carrier.repair"),
                craft(stack(SkofcraftItems.DAMAGE_CONTROL_KIT.get()), new String[] {"GPG", "PIP", "PPP"},
                        'G', SkofcraftItems.GEAR.get(), 'P', SkofcraftItems.AIRCRAFT_PLATE.get(), 'I', Items.IRON_INGOT),
                text("carrier.radar"),
                craft(stack(SkofcraftItems.RADAR.get()), new String[] {" C ", "RAR", "PPP"},
                        'C', Items.COMPASS, 'R', Items.REDSTONE, 'A', SkofcraftItems.AVIONICS.get(), 'P', SkofcraftItems.AIRCRAFT_PLATE.get()),
                text("carrier.defense"),
                craft(stack(SkofcraftItems.CIWS.get()), new String[] {" W ", "GAG", "PBP"},
                        'W', SkofcraftItems.WEAPON_SYSTEM.get(), 'G', SkofcraftItems.GEAR.get(), 'A', SkofcraftItems.AVIONICS.get(),
                        'P', SkofcraftItems.AIRCRAFT_PLATE.get(), 'B', Items.IRON_BLOCK),
                craft(stack(SkofcraftItems.SAM_LAUNCHER.get()), new String[] {"TWT", "PAP", "PBP"},
                        'T', Items.TNT, 'W', SkofcraftItems.WEAPON_SYSTEM.get(), 'A', SkofcraftItems.AVIONICS.get(),
                        'P', SkofcraftItems.AIRCRAFT_PLATE.get(), 'B', Items.IRON_BLOCK)
        )));

        chapters.add(new Chapter(t("space.title"), stack(SkofcraftItems.RECON_SATELLITE.get()), List.of(
                text("space.p1"),
                craft(stack(SkofcraftItems.LAUNCH_PAD.get()), new String[] {"PJP", "GBG", "BBB"},
                        'P', SkofcraftItems.AIRCRAFT_PLATE.get(), 'J', SkofcraftItems.JET_ENGINE.get(),
                        'G', SkofcraftItems.GEAR.get(), 'B', Items.IRON_BLOCK),
                text("space.p2"),
                craft(stack(SkofcraftItems.RECON_SATELLITE.get()), new String[] {"DAD", "PRP", "DAD"},
                        'D', Items.DAYLIGHT_DETECTOR, 'A', SkofcraftItems.AVIONICS.get(),
                        'P', SkofcraftItems.AIRCRAFT_PLATE.get(), 'R', SkofcraftItems.RADAR.get()),
                text("space.p3"),
                craft(stack(SkofcraftItems.STRIKE_SATELLITE.get()), new String[] {"DAD", "PWP", "DND"},
                        'D', Items.DAYLIGHT_DETECTOR, 'A', SkofcraftItems.AVIONICS.get(), 'P', SkofcraftItems.AIRCRAFT_PLATE.get(),
                        'W', SkofcraftItems.WEAPON_SYSTEM.get(), 'N', Items.NETHERITE_INGOT),
                craft(stack(SkofcraftItems.ORBITAL_DESIGNATOR.get()), new String[] {" S ", "RAR", " I "},
                        'S', Items.SPYGLASS, 'R', Items.REDSTONE, 'A', SkofcraftItems.AVIONICS.get(), 'I', Items.IRON_INGOT),
                text("space.p4"),
                text("space.p5"),
                craft(stack(SkofcraftItems.DRACONIC_WARHEAD.get()), new String[] {"BSB", "NDN", "ERE"},
                        'B', Items.DRAGON_BREATH, 'S', Items.NETHER_STAR, 'N', Items.NETHERITE_BLOCK,
                        'D', Items.DRAGON_HEAD, 'E', Items.END_CRYSTAL, 'R', SkofcraftItems.NUCLEAR_REACTOR.get()),
                craft(stack(SkofcraftItems.ICBM_GUIDANCE.get()), new String[] {"ARA", "CEC", "AWA"},
                        'A', SkofcraftItems.AVIONICS.get(), 'R', SkofcraftItems.RADAR.get(), 'C', Items.RECOVERY_COMPASS,
                        'E', Items.ENDER_EYE, 'W', SkofcraftItems.WEAPON_SYSTEM.get()),
                craft(stack(SkofcraftItems.ICBM_BOOSTER.get()), new String[] {"PJP", "PNP", "JJJ"},
                        'P', SkofcraftItems.AIRCRAFT_PLATE.get(), 'J', SkofcraftItems.JET_ENGINE.get(), 'N', Items.NETHERITE_INGOT),
                craft(stack(SkofcraftItems.GIGA_ICBM.get()), new String[] {"PWP", "BGB", "PBP"},
                        'P', SkofcraftItems.AIRCRAFT_PLATE.get(), 'W', SkofcraftItems.DRACONIC_WARHEAD.get(),
                        'G', SkofcraftItems.ICBM_GUIDANCE.get(), 'B', SkofcraftItems.ICBM_BOOSTER.get()),
                text("space.p6"),
                text("space.p7")
        )));

        chapters.add(new Chapter(t("tactical.title"), stack(SkofcraftItems.TACTICAL_HELMET.get()), List.of(
                text("tactical.p1"),
                craft(new ItemStack(SkofcraftItems.COMPOSITE_PLATE.get(), 2), new String[] {"PSP", "SDS", "PSP"},
                        'P', SkofcraftItems.AIRCRAFT_PLATE.get(), 'S', Items.NETHERITE_SCRAP, 'D', Items.DIAMOND),
                craft(stack(SkofcraftItems.TACTICAL_CORE.get()), new String[] {"ANA", "WSW", "ANA"},
                        'A', SkofcraftItems.AVIONICS.get(), 'N', Items.NETHERITE_INGOT,
                        'W', SkofcraftItems.WEAPON_SYSTEM.get(), 'S', Items.NETHER_STAR),
                craft(stack(SkofcraftItems.GAS_FILTER.get()), new String[] {" I ", "CHC", " I "},
                        'I', Items.IRON_INGOT, 'C', Items.CHARCOAL, 'H', Items.HONEYCOMB),
                text("tactical.p2"),
                text("tactical.helmet"),
                craft(stack(SkofcraftItems.TACTICAL_HELMET.get()), new String[] {"PTP", "PHP", "FGF"},
                        'P', SkofcraftItems.COMPOSITE_PLATE.get(), 'T', SkofcraftItems.TACTICAL_CORE.get(),
                        'H', Items.NETHERITE_HELMET, 'F', SkofcraftItems.GAS_FILTER.get(), 'G', Items.TINTED_GLASS),
                text("tactical.chestplate"),
                craft(stack(SkofcraftItems.TACTICAL_CHESTPLATE.get()), new String[] {"PTP", "PCP", "PPP"},
                        'P', SkofcraftItems.COMPOSITE_PLATE.get(), 'T', SkofcraftItems.TACTICAL_CORE.get(),
                        'C', Items.NETHERITE_CHESTPLATE),
                text("tactical.leggings"),
                craft(stack(SkofcraftItems.TACTICAL_LEGGINGS.get()), new String[] {"PTP", "PLP", "P P"},
                        'P', SkofcraftItems.COMPOSITE_PLATE.get(), 'T', SkofcraftItems.TACTICAL_CORE.get(),
                        'L', Items.NETHERITE_LEGGINGS),
                text("tactical.boots"),
                craft(stack(SkofcraftItems.TACTICAL_BOOTS.get()), new String[] {" T ", "PBP", "P P"},
                        'P', SkofcraftItems.COMPOSITE_PLATE.get(), 'T', SkofcraftItems.TACTICAL_CORE.get(),
                        'B', Items.NETHERITE_BOOTS),
                text("tactical.gloves"),
                craft(stack(SkofcraftItems.TACTICAL_GLOVES.get()), new String[] {"PTP", "NLN"},
                        'P', SkofcraftItems.COMPOSITE_PLATE.get(), 'T', SkofcraftItems.TACTICAL_CORE.get(),
                        'N', Items.NETHERITE_INGOT, 'L', Items.LEATHER),
                text("tactical.set"),
                text("tactical.p3")
        )));

        chapters.add(new Chapter(t("blahaj.title"), stack(SkofcraftItems.BLAHAJ.get()), List.of(
                text("blahaj.p1"),
                craft(stack(SkofcraftItems.BLAHAJ.get()), new String[] {"BBB", "WLW", "BDB"},
                        'B', Items.BLUE_WOOL, 'W', Items.WHITE_WOOL, 'L', Items.LIGHT_BLUE_WOOL, 'D', Items.BLACK_DYE),
                text("blahaj.p2")
        )));

        chapters.add(new Chapter(t("wardrobe.title"), stack(SkofcraftItems.CHOKER.get()), List.of(
                text("wardrobe.p1"),
                text("wardrobe.femboys"),
                text("wardrobe.estrogen"),
                craft(new ItemStack(SkofcraftItems.ESTROGEN_PILL.get(), 4), new String[] {"PSL"},
                        'P', Items.PINK_DYE, 'S', Items.SUGAR, 'L', Items.LIGHT_BLUE_DYE),
                text("wardrobe.mask"),
                craft(stack(SkofcraftItems.FACE_MASK.get()), new String[] {"SBS", "BWB"},
                        'S', Items.STRING, 'B', Items.BLACK_WOOL, 'W', Items.WHITE_DYE),
                text("wardrobe.choker"),
                craft(stack(SkofcraftItems.CHOKER.get()), new String[] {"SBS", "L L", " I "},
                        'S', Items.STRING, 'B', Items.BLACK_DYE, 'L', Items.LEATHER, 'I', Items.IRON_NUGGET),
                text("wardrobe.crop_top"),
                craft(stack(SkofcraftItems.CROP_TOP.get()), new String[] {"B B", "BWB"},
                        'B', Items.BLACK_WOOL, 'W', Items.WHITE_DYE),
                text("wardrobe.hand_warmers"),
                craft(stack(SkofcraftItems.HAND_WARMERS.get()), new String[] {"W W", "B B", "B B"},
                        'W', Items.WHITE_DYE, 'B', Items.BLACK_WOOL),
                text("wardrobe.skirt"),
                craft(stack(SkofcraftItems.SKIRT.get()), new String[] {" S ", "BBB", "BWB"},
                        'S', Items.STRING, 'B', Items.BLACK_WOOL, 'W', Items.WHITE_DYE),
                text("wardrobe.thigh_highs"),
                craft(stack(SkofcraftItems.THIGH_HIGHS.get()), new String[] {"WSW", "B B", "B B"},
                        'W', Items.WHITE_DYE, 'S', Items.STRING, 'B', Items.BLACK_WOOL),
                text("wardrobe.cat_ears"),
                craft(stack(SkofcraftItems.CAT_EARS.get()), new String[] {"B B", "P P"},
                        'B', Items.BLACK_WOOL, 'P', Items.PINK_WOOL),
                text("wardrobe.cat_tail"),
                craft(stack(SkofcraftItems.CAT_TAIL.get()), new String[] {"S", "B", "W"},
                        'S', Items.STRING, 'B', Items.BLACK_WOOL, 'W', Items.WHITE_WOOL),
                text("wardrobe.p2")
        )));

        chapters.add(new Chapter(t("tobacconist.title"), stack(SkofcraftItems.TOBACCONIST_COUNTER.get()), List.of(
                text("tobacconist.p1"),
                craft(stack(SkofcraftItems.TOBACCONIST_COUNTER.get()), new String[] {"SSS", "PCP", "PPP"},
                        'S', Items.DARK_OAK_SLAB, 'P', Items.DARK_OAK_PLANKS, 'C', Items.CHEST),
                text("tobacconist.p2"), text("tobacconist.p3"), text("tobacconist.p4")
        )));

        chapters.add(new Chapter(t("addictol.title"), stack(SkofcraftItems.ADDICTOL.get()), List.of(
                text("addictol.p1"),
                craft(stack(SkofcraftItems.ADDICTOL.get()), new String[] {"G G", " M ", "G G"},
                        'G', Items.GOLDEN_APPLE, 'M', Items.MILK_BUCKET),
                text("addictol.p2")
        )));

        chapters.add(new Chapter(t("using.title"), stack(SkofcraftItems.SNUS_POUCH.get()), List.of(
                text("using.p1"), text("using.p2"), text("using.p3"), text("using.p4")
        )));

        return chapters;
    }

    private static Entry feCraft(ItemLike result, ItemLike center) {
        return craft(stack(result), new String[] {"RXR", "RCR", "RRR"},
                'R', Items.REDSTONE, 'X', center, 'C', Items.COPPER_INGOT);
    }

    private static Craft craft(ItemStack result, String[] pattern, Object... keyPairs) {
        Map<Character, ItemLike> key = new HashMap<>();
        for (int i = 0; i + 1 < keyPairs.length; i += 2) {
            key.put((Character) keyPairs[i], (ItemLike) keyPairs[i + 1]);
        }
        int width = pattern[0].length();
        int height = pattern.length;
        ItemStack[] grid = new ItemStack[width * height];
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                ItemLike item = key.get(pattern[row].charAt(col));
                grid[row * width + col] = item == null ? ItemStack.EMPTY : new ItemStack(item);
            }
        }
        return new Craft(grid, width, height, result);
    }

    private static Process process(ItemLike machine, ItemStack output, ItemStack... inputs) {
        return new Process(List.of(inputs), stack(machine), output);
    }

    private static Text text(String key) {
        return new Text(t(key));
    }

    private static ItemStack stack(ItemLike item) {
        return new ItemStack(item);
    }

    private static Component t(String key) {
        return Component.translatable("questbook.skofcraft." + key);
    }
}
