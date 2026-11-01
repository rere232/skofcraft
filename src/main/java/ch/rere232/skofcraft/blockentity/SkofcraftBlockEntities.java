package ch.rere232.skofcraft.blockentity;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.registry.SkofcraftBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SkofcraftBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SkofcraftMod.MODID);

    public static final RegistryObject<BlockEntityType<NuclearReactorBlockEntity>> NUCLEAR_REACTOR = BLOCK_ENTITIES.register("nuclear_reactor",
            () -> BlockEntityType.Builder.of(NuclearReactorBlockEntity::new, SkofcraftBlocks.NUCLEAR_REACTOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<RadarBlockEntity>> RADAR = BLOCK_ENTITIES.register("radar",
            () -> BlockEntityType.Builder.of(RadarBlockEntity::new, SkofcraftBlocks.RADAR.get()).build(null));

    public static final RegistryObject<BlockEntityType<DefenseTurretBlockEntity>> DEFENSE_TURRET = BLOCK_ENTITIES.register("defense_turret",
            () -> BlockEntityType.Builder.of(DefenseTurretBlockEntity::new, SkofcraftBlocks.CIWS.get(), SkofcraftBlocks.SAM_LAUNCHER.get()).build(null));

    public static final RegistryObject<BlockEntityType<LaunchPadBlockEntity>> LAUNCH_PAD = BLOCK_ENTITIES.register("launch_pad",
            () -> BlockEntityType.Builder.of(LaunchPadBlockEntity::new, SkofcraftBlocks.LAUNCH_PAD.get()).build(null));

    public static final RegistryObject<BlockEntityType<FEDryerBlockEntity>> FE_DRYER = BLOCK_ENTITIES.register("fe_dryer",
            () -> BlockEntityType.Builder.of(
                    FEDryerBlockEntity::new,
                    SkofcraftBlocks.FE_DRYER.get(),
                    SkofcraftBlocks.MANUAL_DRYER.get(),
                    SkofcraftBlocks.FE_NICOTINE_EXTRACTOR.get(),
                    SkofcraftBlocks.INDUSTRIAL_EXTRACTOR.get()
            ).build(null));

    public static final RegistryObject<BlockEntityType<FEGrinderBlockEntity>> FE_GRINDER = BLOCK_ENTITIES.register("fe_grinder",
            () -> BlockEntityType.Builder.of(
                    FEGrinderBlockEntity::new,
                    SkofcraftBlocks.FE_GRINDER.get(),
                    SkofcraftBlocks.MANUAL_GRINDER.get()
            ).build(null));

    public static final RegistryObject<BlockEntityType<FEMixerBlockEntity>> FE_MIXER = BLOCK_ENTITIES.register("fe_mixer",
            () -> BlockEntityType.Builder.of(
                    FEMixerBlockEntity::new,
                    SkofcraftBlocks.FE_MIXER.get(),
                    SkofcraftBlocks.MANUAL_MIXER.get(),
                    SkofcraftBlocks.INDUSTRIAL_SNUS_LINE.get()
            ).build(null));

    public static final RegistryObject<BlockEntityType<FEPressBlockEntity>> FE_PRESS = BLOCK_ENTITIES.register("fe_press",
            () -> BlockEntityType.Builder.of(
                    FEPressBlockEntity::new,
                    SkofcraftBlocks.FE_PRESS.get(),
                    SkofcraftBlocks.MANUAL_POUCH_PRESS.get(),
                    SkofcraftBlocks.INDUSTRIAL_POUCH_LINE.get()
            ).build(null));

    public static final RegistryObject<BlockEntityType<ChichaBlockEntity>> CHICHA = BLOCK_ENTITIES.register("chicha",
            () -> BlockEntityType.Builder.of(
                    ChichaBlockEntity::new,
                    SkofcraftBlocks.CHICHA.get()
            ).build(null));

    public static final RegistryObject<BlockEntityType<SkofeeMachineBlockEntity>> SKOFEE_MACHINE = BLOCK_ENTITIES.register("skofee_machine",
            () -> BlockEntityType.Builder.of(
                    SkofeeMachineBlockEntity::new,
                    SkofcraftBlocks.SKOFEE_MACHINE.get()
            ).build(null));

    public static final RegistryObject<BlockEntityType<FEPackagerBlockEntity>> FE_PACKAGER = BLOCK_ENTITIES.register("fe_packager",
            () -> BlockEntityType.Builder.of(
                    FEPackagerBlockEntity::new,
                    SkofcraftBlocks.FE_PACKAGER.get(),
                    SkofcraftBlocks.INDUSTRIAL_PACKAGER.get()
            ).build(null));
}
