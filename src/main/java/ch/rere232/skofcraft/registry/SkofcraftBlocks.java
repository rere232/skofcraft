package ch.rere232.skofcraft.registry;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.block.FEDryerBlock;
import ch.rere232.skofcraft.crop.TobaccoPlantBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SkofcraftBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SkofcraftMod.MODID);

    public static final RegistryObject<Block> TOBACCO_PLANT = BLOCKS.register("tobacco_plant", () -> new TobaccoPlantBlock(BlockBehaviour.Properties.of().noCollission().randomTicks().instabreak().noOcclusion()));

    public static final RegistryObject<Block> MANUAL_DRYER = machine("manual_dryer", MapColor.WOOD);
    public static final RegistryObject<Block> MANUAL_GRINDER = machine("manual_grinder", MapColor.METAL);
    public static final RegistryObject<Block> MANUAL_MIXER = machine("manual_mixer", MapColor.METAL);
    public static final RegistryObject<Block> MANUAL_POUCH_PRESS = machine("manual_pouch_press", MapColor.METAL);

    public static final RegistryObject<Block> FE_DRYER = BLOCKS.register("fe_dryer", () -> new FEDryerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F)));
    public static final RegistryObject<Block> FE_GRINDER = machine("fe_grinder", MapColor.METAL);
    public static final RegistryObject<Block> FE_MIXER = machine("fe_mixer", MapColor.METAL);
    public static final RegistryObject<Block> FE_PRESS = machine("fe_press", MapColor.METAL);
    public static final RegistryObject<Block> FE_NICOTINE_EXTRACTOR = machine("fe_nicotine_extractor", MapColor.METAL);
    public static final RegistryObject<Block> FE_PACKAGER = machine("fe_packager", MapColor.METAL);

    public static final RegistryObject<Block> INDUSTRIAL_SNUS_LINE = machine("industrial_snus_line", MapColor.COLOR_GRAY);
    public static final RegistryObject<Block> INDUSTRIAL_POUCH_LINE = machine("industrial_pouch_line", MapColor.COLOR_GRAY);
    public static final RegistryObject<Block> INDUSTRIAL_PACKAGER = machine("industrial_packager", MapColor.COLOR_GRAY);
    public static final RegistryObject<Block> INDUSTRIAL_EXTRACTOR = machine("industrial_extractor", MapColor.COLOR_GRAY);

    private static RegistryObject<Block> machine(String name, MapColor color) {
        return BLOCKS.register(name, () -> new Block(BlockBehaviour.Properties.of().mapColor(color).strength(3.0F, 6.0F)));
    }
}
