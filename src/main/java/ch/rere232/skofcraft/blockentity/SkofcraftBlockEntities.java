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
            () -> BlockEntityType.Builder.of(FEDryerBlockEntity::new, SkofcraftBlocks.FE_DRYER.get()).build(null));

    public static final RegistryObject<BlockEntityType<FEGrinderBlockEntity>> FE_GRINDER = BLOCK_ENTITIES.register("fe_grinder",
            () -> BlockEntityType.Builder.of(FEGrinderBlockEntity::new, SkofcraftBlocks.FE_GRINDER.get()).build(null));

    public static final RegistryObject<BlockEntityType<FEMixerBlockEntity>> FE_MIXER = BLOCK_ENTITIES.register("fe_mixer",
            () -> BlockEntityType.Builder.of(FEMixerBlockEntity::new, SkofcraftBlocks.FE_MIXER.get()).build(null));

    public static final RegistryObject<BlockEntityType<FEPressBlockEntity>> FE_PRESS = BLOCK_ENTITIES.register("fe_press",
            () -> BlockEntityType.Builder.of(FEPressBlockEntity::new, SkofcraftBlocks.FE_PRESS.get()).build(null));
}
