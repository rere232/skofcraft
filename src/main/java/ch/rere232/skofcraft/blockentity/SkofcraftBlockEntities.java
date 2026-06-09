package ch.rere232.skofcraft.blockentity;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.registry.SkofcraftBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SkofcraftBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SkofcraftMod.MODID);

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
                    SkofcraftBlocks.FE_PACKAGER.get(),
                    SkofcraftBlocks.INDUSTRIAL_POUCH_LINE.get(),
                    SkofcraftBlocks.INDUSTRIAL_PACKAGER.get()
            ).build(null));
}
