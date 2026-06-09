package ch.rere232.skofcraft.registry;

import ch.rere232.skofcraft.SkofcraftMod;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SkofcraftVillagers {
    public static final ResourceKey<PoiType> TOBACCONIST_POI_KEY = ResourceKey.create(
            Registries.POINT_OF_INTEREST_TYPE,
            ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "tobacconist")
    );

    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(ForgeRegistries.POI_TYPES, SkofcraftMod.MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, SkofcraftMod.MODID);

    public static final RegistryObject<PoiType> TOBACCONIST_POI = POI_TYPES.register("tobacconist",
            () -> new PoiType(ImmutableSet.copyOf(SkofcraftBlocks.MANUAL_MIXER.get().getStateDefinition().getPossibleStates()), 1, 1));

    public static final RegistryObject<VillagerProfession> TOBACCONIST = PROFESSIONS.register("tobacconist",
            () -> new VillagerProfession(
                    "tobacconist",
                    holder -> holder.is(TOBACCONIST_POI_KEY),
                    holder -> holder.is(TOBACCONIST_POI_KEY),
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_CLERIC
            ));

    private SkofcraftVillagers() {
    }
}
