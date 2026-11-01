package ch.rere232.skofcraft.registry;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.recipe.AircraftAssemblyRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SkofcraftRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, SkofcraftMod.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, SkofcraftMod.MODID);

    public static final RegistryObject<RecipeType<AircraftAssemblyRecipe>> AIRCRAFT_ASSEMBLY = RECIPE_TYPES.register("aircraft_assembly",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "aircraft_assembly")));
    public static final RegistryObject<RecipeSerializer<AircraftAssemblyRecipe>> AIRCRAFT_ASSEMBLY_SERIALIZER = RECIPE_SERIALIZERS.register("aircraft_assembly",
            AircraftAssemblyRecipe.Serializer::new);
}
