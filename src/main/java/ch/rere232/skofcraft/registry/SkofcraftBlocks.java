package ch.rere232.skofcraft.registry;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.block.AircraftWorkbenchBlock;
import ch.rere232.skofcraft.block.BigRedButtonBlock;
import ch.rere232.skofcraft.block.CarrierHelmBlock;
import ch.rere232.skofcraft.block.DeckFixtureBlock;
import ch.rere232.skofcraft.block.EvilLeverBlock;
import ch.rere232.skofcraft.block.EvilPressurePlateBlock;
import ch.rere232.skofcraft.block.NuclearReactorBlock;
import ch.rere232.skofcraft.block.RadarBlock;
import ch.rere232.skofcraft.block.DefenseTurretBlock;
import ch.rere232.skofcraft.block.LaunchPadBlock;
import ch.rere232.skofcraft.block.FEDryerBlock;
import ch.rere232.skofcraft.block.FEGrinderBlock;
import ch.rere232.skofcraft.block.FEMixerBlock;
import ch.rere232.skofcraft.block.FEPackagerBlock;
import ch.rere232.skofcraft.block.FEPressBlock;
import ch.rere232.skofcraft.block.ChichaBlock;
import ch.rere232.skofcraft.block.BlahajBlock;
import ch.rere232.skofcraft.block.SkofeeMachineBlock;
import ch.rere232.skofcraft.block.SpitPuddleBlock;
import ch.rere232.skofcraft.block.TobacconistCounterBlock;
import ch.rere232.skofcraft.crop.SkofeePlantBlock;
import ch.rere232.skofcraft.crop.TobaccoPlantBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SkofcraftBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SkofcraftMod.MODID);

    public static final RegistryObject<Block> TOBACCO_PLANT = BLOCKS.register("tobacco_plant", () -> new TobaccoPlantBlock(BlockBehaviour.Properties.of().noCollission().randomTicks().instabreak().noOcclusion()));
    public static final RegistryObject<Block> SKOFEE_PLANT = BLOCKS.register("skofee_plant", () -> new SkofeePlantBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak().noOcclusion().sound(SoundType.CROP).pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> SKOFEE_MACHINE = BLOCKS.register("skofee_machine", () -> new SkofeeMachineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.5F, 6.0F).noOcclusion().sound(SoundType.METAL)));
    public static final RegistryObject<Block> AIRCRAFT_WORKBENCH = BLOCKS.register("aircraft_workbench", () -> new AircraftWorkbenchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final RegistryObject<Block> CARRIER_HELM = BLOCKS.register("carrier_helm", () -> new CarrierHelmBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.METAL)));
    public static final RegistryObject<Block> CATAPULT = BLOCKS.register("catapult", () -> new DeckFixtureBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final RegistryObject<Block> ARRESTING_WIRE = BLOCKS.register("arresting_wire", () -> new DeckFixtureBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final RegistryObject<Block> NUCLEAR_REACTOR = BLOCKS.register("nuclear_reactor", () -> new NuclearReactorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(8.0F, 12.0F).requiresCorrectToolForDrops().lightLevel(state -> 15).sound(SoundType.NETHERITE_BLOCK)));
    public static final RegistryObject<Block> RADAR = BLOCKS.register("radar", () -> new RadarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F, 6.0F).requiresCorrectToolForDrops().lightLevel(state -> 7).sound(SoundType.METAL)));
    public static final RegistryObject<Block> CIWS = BLOCKS.register("ciws", () -> new DefenseTurretBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.METAL), DefenseTurretBlock.Weapon.CIWS));
    public static final RegistryObject<Block> SAM_LAUNCHER = BLOCKS.register("sam_launcher", () -> new DefenseTurretBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.METAL), DefenseTurretBlock.Weapon.SAM));
    public static final RegistryObject<Block> LAUNCH_PAD = BLOCKS.register("launch_pad", () -> new LaunchPadBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F, 12.0F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.METAL)));
    public static final RegistryObject<Block> TOBACCONIST_COUNTER = BLOCKS.register("tobacconist_counter", () -> new TobacconistCounterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
    public static final RegistryObject<Block> BIG_RED_BUTTON = BLOCKS.register("big_red_button", () -> new BigRedButtonBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.METAL)));
    public static final RegistryObject<Block> EVIL_PRESSURE_PLATE = BLOCKS.register("evil_pressure_plate", () -> new EvilPressurePlateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).forceSolidOn().requiresCorrectToolForDrops().noCollission().strength(0.5F).pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> EVIL_LEVER = BLOCKS.register("evil_lever", () -> new EvilLeverBlock(BlockBehaviour.Properties.of().noCollission().strength(0.5F).sound(SoundType.STONE).pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> BLAHAJ = BLOCKS.register("blahaj", () -> new BlahajBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(0.8F).noOcclusion().sound(SoundType.WOOL)));
    public static final RegistryObject<Block> CHICHA = BLOCKS.register("chicha", () -> new ChichaBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5F, 3.0F).noOcclusion().sound(SoundType.METAL)));
    public static final RegistryObject<Block> SPIT_PUDDLE = BLOCKS.register("spit_puddle", () -> new SpitPuddleBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).noCollission().noOcclusion().instabreak().replaceable().pushReaction(PushReaction.DESTROY).sound(SoundType.SLIME_BLOCK)));

    public static final RegistryObject<Block> MANUAL_DRYER = BLOCKS.register("manual_dryer", () -> new FEDryerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(3.0F, 6.0F), false));
    public static final RegistryObject<Block> MANUAL_GRINDER = BLOCKS.register("manual_grinder", () -> new FEGrinderBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F), false));
    public static final RegistryObject<Block> MANUAL_MIXER = BLOCKS.register("manual_mixer", () -> new FEMixerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F), false));
    public static final RegistryObject<Block> MANUAL_POUCH_PRESS = BLOCKS.register("manual_pouch_press", () -> new FEPressBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F), false));

    public static final RegistryObject<Block> FE_DRYER = BLOCKS.register("fe_dryer", () -> new FEDryerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F)));
    public static final RegistryObject<Block> FE_GRINDER = BLOCKS.register("fe_grinder", () -> new FEGrinderBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F)));
    public static final RegistryObject<Block> FE_MIXER = BLOCKS.register("fe_mixer", () -> new FEMixerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F)));
    public static final RegistryObject<Block> FE_PRESS = BLOCKS.register("fe_press", () -> new FEPressBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F)));
    public static final RegistryObject<Block> FE_NICOTINE_EXTRACTOR = BLOCKS.register("fe_nicotine_extractor", () -> new FEDryerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F)));
    public static final RegistryObject<Block> FE_PACKAGER = BLOCKS.register("fe_packager", () -> new FEPackagerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F), false));

    public static final RegistryObject<Block> INDUSTRIAL_SNUS_LINE = BLOCKS.register("industrial_snus_line", () -> new FEMixerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(3.0F, 6.0F)));
    public static final RegistryObject<Block> INDUSTRIAL_POUCH_LINE = BLOCKS.register("industrial_pouch_line", () -> new FEPressBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(3.0F, 6.0F)));
    public static final RegistryObject<Block> INDUSTRIAL_PACKAGER = BLOCKS.register("industrial_packager", () -> new FEPackagerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(3.0F, 6.0F), true));
    public static final RegistryObject<Block> INDUSTRIAL_EXTRACTOR = BLOCKS.register("industrial_extractor", () -> new FEDryerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(3.0F, 6.0F)));

    private static RegistryObject<Block> machine(String name, MapColor color) {
        return BLOCKS.register(name, () -> new Block(BlockBehaviour.Properties.of().mapColor(color).strength(3.0F, 6.0F)));
    }
}
