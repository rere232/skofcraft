package ch.rere232.skofcraft.registry;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.block.GagBlock;
import ch.rere232.skofcraft.block.HeartAltarBlock;
import ch.rere232.skofcraft.block.TherapyCouchBlock;
import ch.rere232.skofcraft.item.ClothingItem;
import ch.rere232.skofcraft.item.CottonPuffItem;
import ch.rere232.skofcraft.item.OutfitBoxItem;
import ch.rere232.skofcraft.worldgen.PastelStructures;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.function.Supplier;

/** What Pastel Paradise is built from and what you find there. The terrain itself is data, under data/skofcraft/worldgen. */
public final class SkofcraftPastel {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SkofcraftMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SkofcraftMod.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, SkofcraftMod.MODID);

    /** Soft all the way through: nothing that lands on these takes fall damage. */
    public static final RegistryObject<Block> MARSHMALLOW = soft("marshmallow", MapColor.SNOW, 0.6F, 0.5F);
    public static final RegistryObject<Block> COTTON_CANDY_CLOUD = soft("cotton_candy_cloud", MapColor.COLOR_PINK, 0.3F, 0.95F);
    public static final RegistryObject<Block> HEART_BRICKS = block("heart_bricks",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5F, 6.0F).sound(SoundType.STONE));
    public static final RegistryObject<Block> PASTEL_PLANKS = block("pastel_planks",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.0F, 3.0F).sound(SoundType.WOOD));
    public static final RegistryObject<Block> HEART_LANTERN = block("heart_lantern",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.5F).lightLevel(state -> 15).sound(SoundType.GLASS));

    public static final RegistryObject<Block> THERAPY_COUCH = BLOCKS.register("therapy_couch", () -> new TherapyCouchBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA).strength(1.0F).noOcclusion().sound(SoundType.WOOL)));
    public static final RegistryObject<Block> HEART_ALTAR = BLOCKS.register("heart_altar", () -> new HeartAltarBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(3.0F, 6.0F).lightLevel(state -> 8).sound(SoundType.STONE)));
    public static final RegistryObject<Item> THERAPY_COUCH_ITEM = ITEMS.register("therapy_couch", () -> new BlockItem(THERAPY_COUCH.get(), new Item.Properties()));
    public static final RegistryObject<Item> HEART_ALTAR_ITEM = ITEMS.register("heart_altar", () -> new BlockItem(HEART_ALTAR.get(), new Item.Properties()));

    public static final RegistryObject<Item> HEART_TOKEN = ITEMS.register("heart_token", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> COTTON_CANDY = ITEMS.register("cotton_candy", () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
            .nutrition(2).saturationMod(0.2F).fast().alwaysEat().effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0), 1.0F).build())));
    public static final RegistryObject<Item> ROASTED_MARSHMALLOW = ITEMS.register("roasted_marshmallow", () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
            .nutrition(7).saturationMod(0.8F).build())));
    public static final RegistryObject<Item> COTTON_PUFF = ITEMS.register("cotton_puff", () -> new CottonPuffItem(new Item.Properties().stacksTo(16)));
    /** Drops from The Algorithm. Worn in the charm slot, it stops the phone feeding the habit. */
    public static final RegistryObject<Item> DO_NOT_DISTURB_CHARM = ITEMS.register("do_not_disturb_charm",
            () -> new ClothingItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> PASTEL_SKIRT = clothing("pastel_skirt");
    public static final RegistryObject<Item> STRIPED_THIGH_HIGHS = clothing("striped_thigh_highs");
    public static final RegistryObject<Item> PASTEL_CROP_TOP = clothing("pastel_crop_top");
    public static final RegistryObject<Item> HEART_CHOKER = clothing("heart_choker");
    public static final RegistryObject<Item> BUNNY_EARS = clothing("bunny_ears");
    public static final RegistryObject<Item> FOX_TAIL = clothing("fox_tail");

    public static final RegistryObject<Item> ROYAL_TIARA = regalia("royal_tiara");
    public static final RegistryObject<Item> ROYAL_GOWN = regalia("royal_gown");
    public static final RegistryObject<Item> GLASS_SLIPPERS = regalia("glass_slippers");
    public static final RegistryObject<Item> RIGHTFUL_FIT_BOX = ITEMS.register("rightful_fit_box",
            () -> new OutfitBoxItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC),
                    () -> List.of(ROYAL_TIARA, ROYAL_GOWN, GLASS_SLIPPERS), "tooltip.skofcraft.rightful_fit_box"));

    /** Better than netherite: the sword from the stone. Comes out of the Snus Kingdom's Excalibur and nowhere else. */
    public static final RegistryObject<Item> EXCALIBUR_SWORD = ITEMS.register("excalibur_sword",
            () -> new SwordItem(ExcaliburTier.INSTANCE, 5, -2.4F, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> PASTEL_BOUTIQUE = FEATURES.register("pastel_boutique", PastelStructures.Boutique::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> PASTEL_COTTAGE = FEATURES.register("pastel_cottage", PastelStructures.Cottage::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> HEADPAT_CAFE = FEATURES.register("headpat_cafe", PastelStructures.Cafe::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> PLUSH_TOWER = FEATURES.register("plush_tower", PastelStructures.Tower::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> HOT_SPRING = FEATURES.register("hot_spring", PastelStructures.HotSpring::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> COTTON_CLOUD = FEATURES.register("cotton_cloud", PastelStructures.CottonCloud::new);

    private SkofcraftPastel() {}

    /** All three pieces of The Rightful Fit, worn at once. */
    public static boolean isWearingRegalia(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(curios -> curios.isEquipped(ROYAL_TIARA.get()) && curios.isEquipped(ROYAL_GOWN.get()) && curios.isEquipped(GLASS_SLIPPERS.get()))
                .orElse(false);
    }

    private static RegistryObject<Block> block(String name, Supplier<BlockBehaviour.Properties> properties) {
        RegistryObject<Block> block = BLOCKS.register(name, () -> new Block(properties.get()));
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegistryObject<Block> soft(String name, MapColor color, float strength, float bounce) {
        RegistryObject<Block> block = BLOCKS.register(name, () -> new GagBlock(name, new GagBlock.Spec(
                BlockBehaviour.Properties.of().mapColor(color).strength(strength).sound(SoundType.WOOL)).bounce(bounce)));
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    public static boolean isWearingCharm(Player player) {
        return CuriosApi.getCuriosInventory(player).map(curios -> curios.isEquipped(DO_NOT_DISTURB_CHARM.get())).orElse(false);
    }

    private static RegistryObject<Item> clothing(String name) {
        return ITEMS.register(name, () -> new ClothingItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    }

    private static RegistryObject<Item> regalia(String name) {
        return ITEMS.register(name, () -> new ClothingItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    }

    private enum ExcaliburTier implements Tier {
        INSTANCE;

        @Override
        public int getUses() {
            return 4000;
        }

        @Override
        public float getSpeed() {
            return 10.0F;
        }

        @Override
        public float getAttackDamageBonus() {
            return 6.0F;
        }

        @Override
        public int getLevel() {
            return 4;
        }

        @Override
        public int getEnchantmentValue() {
            return 22;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(net.minecraft.world.item.Items.NETHERITE_INGOT);
        }

        @Override
        public net.minecraft.tags.TagKey<Block> getTag() {
            return BlockTags.NEEDS_DIAMOND_TOOL;
        }
    }
}
