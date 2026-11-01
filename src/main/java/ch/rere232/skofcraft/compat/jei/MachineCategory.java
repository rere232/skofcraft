package ch.rere232.skofcraft.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;

/** A machine step: up to three inputs in a row, an arrow, and the output. Shared by every SKOFCRAFT machine. */
public class MachineCategory implements IRecipeCategory<MachineRecipe> {
    private static final int WIDTH = 120;
    private static final int HEIGHT = 28;
    private static final int ARROW_X = 60;
    private static final int OUTPUT_X = 96;

    private final RecipeType<MachineRecipe> type;
    private final Component title;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public MachineCategory(IGuiHelper guiHelper, RecipeType<MachineRecipe> type, ItemLike icon) {
        this.type = type;
        this.title = Component.translatable("jei.skofcraft." + type.getUid().getPath());
        this.icon = guiHelper.createDrawableItemLike(icon);
        this.arrow = guiHelper.createAnimatedRecipeArrow(80);
    }

    @Override
    public RecipeType<MachineRecipe> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MachineRecipe recipe, IFocusGroup focuses) {
        IRecipeSlotBuilder linked = null;
        for (int i = 0; i < recipe.inputs().size(); i++) {
            List<ItemStack> stacks = recipe.inputs().get(i);
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, 1 + i * 18, 6)
                    .setStandardSlotBackground()
                    .addItemStacks(stacks);
            if (i == recipe.linkedInput()) {
                linked = slot;
            }
        }
        IRecipeSlotBuilder output = builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X + 4, 6)
                .setOutputSlotBackground()
                .addItemStacks(recipe.outputs());
        if (linked != null) {
            // Keep the flavor and the flavored product in step while JEI cycles them
            builder.createFocusLink(linked, output);
        }
    }

    @Override
    public void draw(MachineRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, ARROW_X, 6);
    }
}
