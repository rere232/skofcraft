package ch.rere232.skofcraft.registry;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.block.SkofeeFluidBlock;
import ch.rere232.skofcraft.client.SkofeeFluidClient;
import ch.rere232.skofcraft.worldgen.KingdomFeatures;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Everything the Snus Kingdom is built from. The rest of the dimension is data, under data/skofcraft/worldgen. */
public final class SkofcraftKingdom {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SkofcraftMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SkofcraftMod.MODID);
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, SkofcraftMod.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, SkofcraftMod.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, SkofcraftMod.MODID);

    public static final RegistryObject<Block> PACKED_SNUS = block("packed_snus",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(1.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE));
    public static final RegistryObject<Block> TIN_PLATE = block("tin_plate",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL));
    public static final RegistryObject<Block> TIN_LID = block("tin_lid",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL));
    public static final RegistryObject<Block> POUCH_FABRIC = block("pouch_fabric",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.8F).sound(SoundType.WOOL));
    public static final RegistryObject<Block> ROASTED_GROUNDS = block("roasted_grounds",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.5F).sound(SoundType.SAND));
    public static final RegistryObject<Block> CIGAR_WRAPPER = block("cigar_wrapper",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(2.0F).sound(SoundType.WOOD));
    public static final RegistryObject<Block> CIGAR_EMBER = block("cigar_ember",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.5F).lightLevel(state -> 12).sound(SoundType.WOOL));

    public static final RegistryObject<FluidType> SKOFEE_TYPE = FLUID_TYPES.register("skofee", () -> new FluidType(
            FluidType.Properties.create().descriptionId("block.skofcraft.liquid_skofee").density(1100).viscosity(1400)) {
        @Override
        public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
            consumer.accept(new SkofeeFluidClient());
        }
    });
    public static final RegistryObject<FlowingFluid> SKOFEE = FLUIDS.register("skofee", () -> new ForgeFlowingFluid.Source(skofeeProperties()));
    public static final RegistryObject<FlowingFluid> FLOWING_SKOFEE = FLUIDS.register("flowing_skofee", () -> new ForgeFlowingFluid.Flowing(skofeeProperties()));
    public static final RegistryObject<LiquidBlock> SKOFEE_BLOCK = BLOCKS.register("liquid_skofee", () -> new SkofeeFluidBlock(SKOFEE,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).replaceable().noCollission().strength(100.0F)
                    .pushReaction(PushReaction.DESTROY).noLootTable().liquid().sound(SoundType.EMPTY)));
    public static final RegistryObject<Item> SKOFEE_BUCKET = ITEMS.register("skofee_bucket",
            () -> new BucketItem(SKOFEE, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> GIANT_SNUS_TIN = FEATURES.register("giant_snus_tin", KingdomFeatures.GiantSnusTin::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> CIGAR_TREE = FEATURES.register("cigar_tree", KingdomFeatures.CigarTree::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> POUCH_BOULDER = FEATURES.register("pouch_boulder", KingdomFeatures.PouchBoulder::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> TOBACCO_FIELD = FEATURES.register("tobacco_field", () -> new KingdomFeatures.CropField(SkofcraftBlocks.TOBACCO_PLANT));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SKOFEE_FIELD = FEATURES.register("skofee_field", () -> new KingdomFeatures.CropField(SkofcraftBlocks.SKOFEE_PLANT));

    private SkofcraftKingdom() {}

    private static ForgeFlowingFluid.Properties skofeeProperties() {
        return new ForgeFlowingFluid.Properties(SKOFEE_TYPE, SKOFEE, FLOWING_SKOFEE)
                .block(SKOFEE_BLOCK).bucket(SKOFEE_BUCKET).tickRate(8).slopeFindDistance(3).levelDecreasePerBlock(2);
    }

    private static RegistryObject<Block> block(String name, Supplier<BlockBehaviour.Properties> properties) {
        RegistryObject<Block> block = BLOCKS.register(name, () -> new Block(properties.get()));
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }
}
