package ch.rere232.skofcraft.compat.jei;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.config.SkofcraftConfig;
import ch.rere232.skofcraft.item.GumConsumableItem;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import ch.rere232.skofcraft.registry.SkofcraftRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * JEI support. The tobacco machines decide their output in code rather than through recipe JSON, so their steps are
 * listed here by hand and must follow the block entities' logic (FEDryer, FEGrinder, FEMixer, FEPress, FEPackager and
 * the SKOFee machine). Aircraft Workbench recipes are real recipes and come from the recipe manager.
 */
@JeiPlugin
public class SkofcraftJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "jei");

    static final RecipeType<MachineRecipe> DRYING = type("drying");
    static final RecipeType<MachineRecipe> EXTRACTION = type("extraction");
    static final RecipeType<MachineRecipe> GRINDING = type("grinding");
    static final RecipeType<MachineRecipe> MIXING = type("mixing");
    static final RecipeType<MachineRecipe> PRESSING = type("pressing");
    static final RecipeType<MachineRecipe> PACKAGING = type("packaging");
    static final RecipeType<MachineRecipe> BREWING = type("brewing");

    /** Flavor item to its quest book line, which doubles as its JEI info page. */
    private static final Map<RegistryObject<Item>, String> FLAVORS = Map.of(
            SkofcraftItems.FLAVOR_MINT, "mint", SkofcraftItems.FLAVOR_ICE_MINT, "ice_mint",
            SkofcraftItems.FLAVOR_BERRY, "berry", SkofcraftItems.FLAVOR_CITRUS, "citrus",
            SkofcraftItems.FLAVOR_COLA, "cola", SkofcraftItems.FLAVOR_LICORICE, "licorice",
            SkofcraftItems.FLAVOR_COFFEE, "coffee", SkofcraftItems.FLAVOR_NATURE, "nature");

    private static RecipeType<MachineRecipe> type(String path) {
        return RecipeType.create(SkofcraftMod.MODID, path, MachineRecipe.class);
    }

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new MachineCategory(gui, DRYING, SkofcraftItems.MANUAL_DRYER.get()),
                new MachineCategory(gui, EXTRACTION, SkofcraftItems.FE_NICOTINE_EXTRACTOR.get()),
                new MachineCategory(gui, GRINDING, SkofcraftItems.MANUAL_GRINDER.get()),
                new MachineCategory(gui, MIXING, SkofcraftItems.MANUAL_MIXER.get()),
                new MachineCategory(gui, PRESSING, SkofcraftItems.MANUAL_POUCH_PRESS.get()),
                new MachineCategory(gui, PACKAGING, SkofcraftItems.FE_PACKAGER.get()),
                new MachineCategory(gui, BREWING, SkofcraftItems.SKOFEE_MACHINE.get()),
                new AircraftAssemblyCategory(gui));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(DRYING, SkofcraftItems.MANUAL_DRYER.get(), SkofcraftItems.FE_DRYER.get());
        registration.addRecipeCatalysts(EXTRACTION, SkofcraftItems.FE_NICOTINE_EXTRACTOR.get(), SkofcraftItems.INDUSTRIAL_EXTRACTOR.get());
        registration.addRecipeCatalysts(GRINDING, SkofcraftItems.MANUAL_GRINDER.get(), SkofcraftItems.FE_GRINDER.get());
        registration.addRecipeCatalysts(MIXING, SkofcraftItems.MANUAL_MIXER.get(), SkofcraftItems.FE_MIXER.get(),
                SkofcraftItems.INDUSTRIAL_SNUS_LINE.get());
        registration.addRecipeCatalysts(PRESSING, SkofcraftItems.MANUAL_POUCH_PRESS.get(), SkofcraftItems.FE_PRESS.get(),
                SkofcraftItems.INDUSTRIAL_POUCH_LINE.get());
        registration.addRecipeCatalysts(PACKAGING, SkofcraftItems.FE_PACKAGER.get(), SkofcraftItems.INDUSTRIAL_PACKAGER.get());
        registration.addRecipeCatalysts(BREWING, SkofcraftItems.SKOFEE_MACHINE.get());
        registration.addRecipeCatalysts(AircraftAssemblyCategory.TYPE, SkofcraftItems.AIRCRAFT_WORKBENCH.get());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(DRYING, List.of(
                MachineRecipe.of(stack(SkofcraftItems.TOBACCO_LEAF_DRY), stack(SkofcraftItems.TOBACCO_LEAF_FRESH)),
                MachineRecipe.of(stack(SkofcraftItems.SKOFEE_BEANS_DRY), stack(SkofcraftItems.SKOFEE_BEANS_FRESH))));
        registration.addRecipes(EXTRACTION, List.of(
                MachineRecipe.of(stack(SkofcraftItems.NICOTINE_EXTRACT), stack(SkofcraftItems.TOBACCO_LEAF_DRY))));
        registration.addRecipes(GRINDING, List.of(
                MachineRecipe.of(new ItemStack(SkofcraftItems.TOBACCO_DUST.get(), 2), stack(SkofcraftItems.TOBACCO_LEAF_DRY)),
                MachineRecipe.of(new ItemStack(SkofcraftItems.SKOFEE_GROUND.get(), 2), stack(SkofcraftItems.SKOFEE_BEANS_ROASTED))));
        registration.addRecipes(MIXING, mixing());
        registration.addRecipes(PRESSING, pressing());

        int capacity = Math.max(1, SkofcraftConfig.boxCapacity);
        registration.addRecipes(PACKAGING, List.of(
                MachineRecipe.of(stack(SkofcraftItems.FILLED_SNUS_BOX),
                        new ItemStack(SkofcraftItems.SNUS_POUCH.get(), capacity), stack(SkofcraftItems.EMPTY_SNUS_BOX)),
                MachineRecipe.of(stack(SkofcraftItems.FILLED_POUCH_BOX),
                        new ItemStack(SkofcraftItems.NICOTINE_POUCH.get(), capacity), stack(SkofcraftItems.EMPTY_POUCH_BOX)),
                MachineRecipe.of(stack(SkofcraftItems.CIGARETTE_PACK),
                        new ItemStack(SkofcraftItems.CIGARETTE.get(), capacity), stack(SkofcraftItems.EMPTY_CIGARETTE_PACK))));
        registration.addRecipes(BREWING, List.of(
                MachineRecipe.of(stack(SkofcraftItems.SKOFEE),
                        stack(SkofcraftItems.SKOFEE_CUP), stack(SkofcraftItems.SKOFEE_GROUND), new ItemStack(Items.WATER_BUCKET))));

        var level = Minecraft.getInstance().level;
        if (level != null) {
            registration.addRecipes(AircraftAssemblyCategory.TYPE,
                    level.getRecipeManager().getAllRecipesFor(SkofcraftRecipes.AIRCRAFT_ASSEMBLY.get()));
        }

        FLAVORS.forEach((flavor, id) -> registration.addIngredientInfo(flavor.get(),
                Component.translatable("questbook.skofcraft.flavors." + id),
                Component.translatable("questbook.skofcraft.flavors.p1")));
        registration.addIngredientInfo(SkofcraftItems.DOOMSDAY_MISSILE.get(), Component.translatable("questbook.skofcraft.space.p7"));
        registration.addIngredientInfo(SkofcraftItems.TACTICAL_GLOVES.get(), Component.translatable("questbook.skofcraft.tactical.gloves"));
    }

    /** Mixer: neutral base + nicotine + flavor makes a mix (crystals make it stronger), dust + flavor makes snuff, a mix fills a vape. */
    private static List<MachineRecipe> mixing() {
        List<ItemStack> flavors = flavors();
        List<MachineRecipe> recipes = new ArrayList<>();
        ItemStack[] nicotine = {stack(SkofcraftItems.NICOTINE_EXTRACT), stack(SkofcraftItems.NICOTINE_CRYSTALS),
                new ItemStack(SkofcraftItems.NICOTINE_CRYSTALS.get(), 2)};
        for (int strength = 0; strength < nicotine.length; strength++) {
            int s = strength;
            recipes.add(new MachineRecipe(
                    List.of(List.of(stack(SkofcraftItems.NEUTRAL_BASE)), List.of(nicotine[strength]), flavors),
                    flavored(SkofcraftItems.NICOTINE_MIX, flavors, mix -> GumConsumableItem.setStrength(mix, s)), 2));
        }
        recipes.add(new MachineRecipe(List.of(List.of(stack(SkofcraftItems.TOBACCO_DUST)), List.of(), flavors),
                flavored(SkofcraftItems.SNUFF, flavors, snuff -> {}), 2));
        List<ItemStack> mixes = flavored(SkofcraftItems.NICOTINE_MIX, flavors, mix -> {});
        recipes.add(new MachineRecipe(List.of(List.of(stack(SkofcraftItems.EMPTY_VAPE)), mixes),
                flavored(SkofcraftItems.VAPE, flavors, vape -> {}), 1));
        return recipes;
    }

    /** Press: dust + empty pouch makes snus, stronger with a booster; a nicotine mix + empty pouch makes a nicotine pouch. */
    private static List<MachineRecipe> pressing() {
        List<ItemStack> flavors = flavors();
        List<MachineRecipe> recipes = new ArrayList<>();
        ItemStack[] boosters = {ItemStack.EMPTY, stack(SkofcraftItems.NICOTINE_EXTRACT), stack(SkofcraftItems.NICOTINE_CRYSTALS),
                new ItemStack(SkofcraftItems.NICOTINE_CRYSTALS.get(), 2)};
        for (int strength = 0; strength < boosters.length; strength++) {
            ItemStack snus = stack(SkofcraftItems.SNUS_POUCH);
            GumConsumableItem.setStrength(snus, strength);
            recipes.add(MachineRecipe.of(snus, stack(SkofcraftItems.TOBACCO_DUST), stack(SkofcraftItems.EMPTY_POUCH), boosters[strength]));
        }
        recipes.add(new MachineRecipe(
                List.of(flavored(SkofcraftItems.NICOTINE_MIX, flavors, mix -> {}), List.of(stack(SkofcraftItems.EMPTY_POUCH))),
                flavored(SkofcraftItems.NICOTINE_POUCH, flavors, pouch -> {}), 0));
        return recipes;
    }

    private static List<ItemStack> flavors() {
        return FLAVORS.keySet().stream().map(SkofcraftJeiPlugin::stack)
                .sorted(java.util.Comparator.comparing(s -> s.getItem().getDescriptionId())).toList();
    }

    /** One {@code item} per flavor, in the same order as {@code flavors}, so a focus link keeps them in step. */
    private static List<ItemStack> flavored(RegistryObject<Item> item, List<ItemStack> flavors, java.util.function.Consumer<ItemStack> extra) {
        Function<ItemStack, ItemStack> make = flavor -> {
            ItemStack stack = stack(item);
            GumConsumableItem.setFlavor(stack, flavor);
            extra.accept(stack);
            return stack;
        };
        return flavors.stream().map(make).toList();
    }

    private static ItemStack stack(RegistryObject<? extends ItemLike> item) {
        return new ItemStack(item.get());
    }
}
