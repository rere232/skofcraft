package ch.rere232.skofcraft.compat.jei;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.recipe.AircraftAssemblyRecipe;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;

/** The Aircraft Workbench's 5 by 4 shaped grid. */
public class AircraftAssemblyCategory implements IRecipeCategory<AircraftAssemblyRecipe> {
    public static final RecipeType<AircraftAssemblyRecipe> TYPE =
            RecipeType.create(SkofcraftMod.MODID, "aircraft_assembly", AircraftAssemblyRecipe.class);
    private static final int GRID_W = AircraftAssemblyRecipe.MAX_WIDTH * 18;
    private static final int GRID_H = AircraftAssemblyRecipe.MAX_HEIGHT * 18;
    private static final int ARROW_X = GRID_W + 8;
    private static final int OUTPUT_X = ARROW_X + 32;

    private final IDrawable icon;
    private final IDrawableStatic arrow;

    public AircraftAssemblyCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(SkofcraftItems.AIRCRAFT_WORKBENCH.get());
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<AircraftAssemblyRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.skofcraft.aircraft_assembly");
    }

    @Override
    public int getWidth() {
        return OUTPUT_X + 26;
    }

    @Override
    public int getHeight() {
        return GRID_H;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AircraftAssemblyRecipe recipe, IFocusGroup focuses) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        // Every cell gets a slot so the whole grid shows; the pattern sits in the top left like on the workbench
        for (int row = 0; row < AircraftAssemblyRecipe.MAX_HEIGHT; row++) {
            for (int col = 0; col < AircraftAssemblyRecipe.MAX_WIDTH; col++) {
                var slot = builder.addSlot(RecipeIngredientRole.INPUT, col * 18 + 1, row * 18 + 1).setStandardSlotBackground();
                if (col < recipe.getWidth() && row < recipe.getHeight()) {
                    slot.addIngredients(ingredients.get(row * recipe.getWidth() + col));
                }
            }
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X + 4, GRID_H / 2 - 8)
                .setOutputSlotBackground()
                .addItemStack(recipe.getResultItem(Minecraft.getInstance().level.registryAccess()));
    }

    @Override
    public void draw(AircraftAssemblyRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, ARROW_X, GRID_H / 2 - 8);
    }
}
