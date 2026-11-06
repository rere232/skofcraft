package ch.rere232.skofcraft;

import ch.rere232.skofcraft.blockentity.SkofcraftBlockEntities;
import ch.rere232.skofcraft.carrier.CarrierEvents;
import ch.rere232.skofcraft.config.SkofcraftConfig;
import ch.rere232.skofcraft.gums.GumEvents;
import ch.rere232.skofcraft.menu.SkofcraftMenus;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import ch.rere232.skofcraft.registry.SkofcraftBlocks;
import ch.rere232.skofcraft.registry.SkofcraftCreativeTabs;
import ch.rere232.skofcraft.registry.SkofcraftEffects;
import ch.rere232.skofcraft.registry.SkofcraftEntities;
import ch.rere232.skofcraft.registry.SkofcraftGagBlocks;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import ch.rere232.skofcraft.registry.SkofcraftKingdom;
import ch.rere232.skofcraft.registry.SkofcraftPastel;
import ch.rere232.skofcraft.registry.SkofcraftRecipes;
import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.registry.SkofcraftStructures;
import ch.rere232.skofcraft.registry.SkofcraftVillagers;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(SkofcraftMod.MODID)
public class SkofcraftMod {
    public static final String MODID = "skofcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SkofcraftMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        SkofcraftBlocks.BLOCKS.register(modEventBus);
        SkofcraftGagBlocks.BLOCKS.register(modEventBus);
        SkofcraftKingdom.BLOCKS.register(modEventBus);
        SkofcraftPastel.BLOCKS.register(modEventBus);
        SkofcraftPastel.FEATURES.register(modEventBus);
        SkofcraftKingdom.FLUID_TYPES.register(modEventBus);
        SkofcraftKingdom.FLUIDS.register(modEventBus);
        SkofcraftKingdom.FEATURES.register(modEventBus);
        SkofcraftEffects.EFFECTS.register(modEventBus);
        SkofcraftEntities.ENTITIES.register(modEventBus);
        SkofcraftStructures.STRUCTURE_TYPES.register(modEventBus);
        SkofcraftSounds.SOUNDS.register(modEventBus);
        SkofcraftItems.ITEMS.register(modEventBus);
        SkofcraftGagBlocks.ITEMS.register(modEventBus);
        SkofcraftKingdom.ITEMS.register(modEventBus);
        SkofcraftPastel.ITEMS.register(modEventBus);
        SkofcraftCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        SkofcraftBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        SkofcraftMenus.MENUS.register(modEventBus);
        SkofcraftRecipes.RECIPE_TYPES.register(modEventBus);
        SkofcraftRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        SkofcraftVillagers.POI_TYPES.register(modEventBus);
        SkofcraftVillagers.PROFESSIONS.register(modEventBus);
        SkofcraftNetwork.register();

        context.registerConfig(ModConfig.Type.COMMON, SkofcraftConfig.SPEC, "skofcraft-common.toml");
        context.registerConfig(ModConfig.Type.CLIENT, ch.rere232.skofcraft.config.SkofcraftClientConfig.SPEC, "skofcraft-client.toml");

        MinecraftForge.EVENT_BUS.register(new GumEvents());
        MinecraftForge.EVENT_BUS.register(new CarrierEvents());

        LOGGER.info("SKOFCRAFT initialized");
    }
}
