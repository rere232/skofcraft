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
    record Craft(ItemStack[] grid, ItemStack result) implements Entry {}

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
                text("nicotine.p4")
        )));

        int capacity = Math.max(1, SkofcraftConfig.boxCapacity);
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
        ItemStack[] grid = new ItemStack[9];
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                ItemLike item = key.get(pattern[row].charAt(col));
                grid[row * 3 + col] = item == null ? ItemStack.EMPTY : new ItemStack(item);
            }
        }
        return new Craft(grid, result);
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
