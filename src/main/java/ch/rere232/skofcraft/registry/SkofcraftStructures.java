package ch.rere232.skofcraft.registry;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.worldgen.FlatGroundStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class SkofcraftStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, SkofcraftMod.MODID);

    public static final RegistryObject<StructureType<FlatGroundStructure>> FLAT_GROUND = STRUCTURE_TYPES.register("flat_ground",
            () -> () -> FlatGroundStructure.CODEC);

    private SkofcraftStructures() {}
}
