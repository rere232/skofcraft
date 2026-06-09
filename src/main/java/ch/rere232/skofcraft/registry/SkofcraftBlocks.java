package ch.rere232.skofcraft.registry;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.block.FEDryerBlock;
import ch.rere232.skofcraft.block.FEGrinderBlock;
import ch.rere232.skofcraft.block.FEMixerBlock;
import ch.rere232.skofcraft.block.FEPressBlock;
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

    public static final RegistryObject<Block> MANUAL_DRYER = BLOCKS.register("manual_dryer", () -> new FEDryerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(3.0F, 6.0F)));
    public static final RegistryObject<Block> MANUAL_GRINDER = BLOCKS.register("manual_grinder", () -> new FEGrinderBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F)));
    public static final RegistryObject<Block> MANUAL_MIXER = BLOCKS.register("manual_mixer", () -> new FEMixerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F)));
    public static final RegistryObject<Block> MANUAL_POUCH_PRESS = BLOCKS.register("manual_pouch_press", () -> new FEPressBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F)));

    public static final RegistryObject<Block> FE_DRYER = BLOCKS.register("fe_dryer", () -> new FEDryerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F)));
    public static final RegistryObject<Block> FE_GRINDER = BLOCKS.register("fe_grinder", () -> new FEGrinderBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F)));
    public static final RegistryObject<Block> FE_MIXER = BLOCKS.register("fe_mixer", () -> new FEMixerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F)));
    public static final RegistryObject<Block> FE_PRESS = BLOCKS.register("fe_press", () -> new FEPressBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F)));
    public static final RegistryObject<Block> FE_NICOTINE_EXTRACTOR = BLOCKS.register("fe_nicotine_extractor", () -> new FEDryerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F)));
    public static final RegistryObject<Block> FE_PACKAGER = BLOCKS.register("fe_packager", () -> new FEPressBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F)));

    public static final RegistryObject<Block> INDUSTRIAL_SNUS_LINE = BLOCKS.register("industrial_snus_line", () -> new FEMixerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(3.0F, 6.0F)));
    public static final RegistryObject<Block> INDUSTRIAL_POUCH_LINE = BLOCKS.register("industrial_pouch_line", () -> new FEPressBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(3.0F, 6.0F)));
    public static final RegistryObject<Block> INDUSTRIAL_PACKAGER = BLOCKS.register("industrial_packager", () -> new FEPressBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(3.0F, 6.0F)));
    public static final RegistryObject<Block> INDUSTRIAL_EXTRACTOR = BLOCKS.register("industrial_extractor", () -> new FEDryerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(3.0F, 6.0F)));

    private static RegistryObject<Block> machine(String name, MapColor color) {
        return BLOCKS.register(name, () -> new Block(BlockBehaviour.Properties.of().mapColor(color).strength(3.0F, 6.0F)));
    }
}
