package ch.rere232.skofcraft.recipe;

import ch.rere232.skofcraft.registry.SkofcraftRecipes;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Shaped recipe for the Aircraft Workbench's 5x4 grid. Same JSON layout as a vanilla shaped recipe
 * ("pattern", "key", "result"), with patterns up to 5 wide and 4 tall.
 */
public class AircraftAssemblyRecipe implements Recipe<CraftingContainer> {
    public static final int MAX_WIDTH = 5;
    public static final int MAX_HEIGHT = 4;

    private final ResourceLocation id;
    private final int width;
    private final int height;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;

    public AircraftAssemblyRecipe(ResourceLocation id, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.ingredients = ingredients;
        this.result = result;
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        for (int x = 0; x <= container.getWidth() - width; x++) {
            for (int y = 0; y <= container.getHeight() - height; y++) {
                if (matches(container, x, y, true) || matches(container, x, y, false)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matches(CraftingContainer container, int offsetX, int offsetY, boolean mirrored) {
        for (int x = 0; x < container.getWidth(); x++) {
            for (int y = 0; y < container.getHeight(); y++) {
                int px = x - offsetX;
                int py = y - offsetY;
                Ingredient ingredient = Ingredient.EMPTY;
                if (px >= 0 && py >= 0 && px < width && py < height) {
                    ingredient = ingredients.get((mirrored ? width - px - 1 : px) + py * width);
                }
                if (!ingredient.test(container.getItem(x + y * container.getWidth()))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int gridWidth, int gridHeight) {
        return gridWidth >= width && gridHeight >= height;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /** Kept out of the vanilla recipe book, which has no tab for it. */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SkofcraftRecipes.AIRCRAFT_ASSEMBLY_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return SkofcraftRecipes.AIRCRAFT_ASSEMBLY.get();
    }

    public static class Serializer implements RecipeSerializer<AircraftAssemblyRecipe> {
        @Override
        public AircraftAssemblyRecipe fromJson(ResourceLocation id, JsonObject json) {
            Map<Character, Ingredient> key = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : GsonHelper.getAsJsonObject(json, "key").entrySet()) {
                if (entry.getKey().length() != 1 || entry.getKey().equals(" ")) {
                    throw new JsonParseException("Invalid key '" + entry.getKey() + "' in " + id);
                }
                key.put(entry.getKey().charAt(0), Ingredient.fromJson(entry.getValue(), false));
            }

            JsonArray pattern = GsonHelper.getAsJsonArray(json, "pattern");
            int height = pattern.size();
            if (height == 0 || height > MAX_HEIGHT) {
                throw new JsonParseException("Pattern of " + id + " must have 1 to " + MAX_HEIGHT + " rows");
            }
            int width = GsonHelper.convertToString(pattern.get(0), "pattern[0]").length();
            if (width == 0 || width > MAX_WIDTH) {
                throw new JsonParseException("Pattern of " + id + " must be 1 to " + MAX_WIDTH + " wide");
            }
            NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
            for (int row = 0; row < height; row++) {
                String line = GsonHelper.convertToString(pattern.get(row), "pattern[" + row + "]");
                if (line.length() != width) {
                    throw new JsonParseException("Pattern rows of " + id + " must all be the same width");
                }
                for (int col = 0; col < width; col++) {
                    char c = line.charAt(col);
                    if (c == ' ') {
                        continue;
                    }
                    Ingredient ingredient = key.get(c);
                    if (ingredient == null) {
                        throw new JsonParseException("Pattern of " + id + " uses undefined key '" + c + "'");
                    }
                    ingredients.set(col + row * width, ingredient);
                }
            }

            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            return new AircraftAssemblyRecipe(id, width, height, ingredients, result);
        }

        @Override
        public AircraftAssemblyRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            int width = buf.readVarInt();
            int height = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
            ingredients.replaceAll(ignored -> Ingredient.fromNetwork(buf));
            return new AircraftAssemblyRecipe(id, width, height, ingredients, buf.readItem());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, AircraftAssemblyRecipe recipe) {
            buf.writeVarInt(recipe.width);
            buf.writeVarInt(recipe.height);
            for (Ingredient ingredient : recipe.ingredients) {
                ingredient.toNetwork(buf);
            }
            buf.writeItem(recipe.result);
        }
    }
}
