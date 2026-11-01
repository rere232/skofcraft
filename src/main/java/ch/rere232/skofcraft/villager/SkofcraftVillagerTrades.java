package ch.rere232.skofcraft.villager;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.config.SkofcraftConfig;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import ch.rere232.skofcraft.registry.SkofcraftVillagers;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.function.Supplier;

/**
 * Tobacconist trades. A villager rolls two listings per level, so each level has exactly
 * two bundles and every Tobacconist gets all the offers in them.
 */
@Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SkofcraftVillagerTrades {
    private static final String STRENGTH_TAG = "SkofcraftStrength";

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (!SkofcraftConfig.enableVillager || event.getType() != SkofcraftVillagers.TOBACCONIST.get()) {
            return;
        }

        List<VillagerTrades.ItemListing> level1 = event.getTrades().get(1);
        level1.add(bundle(
                sell(SkofcraftItems.TOBACCO_SEEDS, 8, 1, 16, 2),
                buy(SkofcraftItems.TOBACCO_LEAF_FRESH, 12, 16, 2),
                buy(SkofcraftItems.VEGETAL_FIBER, 10, 16, 2)));
        level1.add(bundle(
                sell(SkofcraftItems.EMPTY_POUCH, 8, 2, 12, 2),
                swap(Items.EMERALD, 2, Items.BOOK, 1, SkofcraftItems.QUEST_BOOK, 1, 8, 5)));

        List<VillagerTrades.ItemListing> level2 = event.getTrades().get(2);
        level2.add(bundle(
                sell(SkofcraftItems.MANUAL_DRYER, 1, 6, 6, 10),
                buy(SkofcraftItems.TOBACCO_LEAF_DRY, 16, 12, 10),
                sell(SkofcraftItems.EMPTY_CIGARETTE_PACK, 2, 3, 12, 8)));
        level2.add(bundle(
                sell(SkofcraftItems.CIGARETTE, 4, 1, 16, 8),
                sell(SkofcraftItems.FLAVOR_MINT, 2, 4, 8, 8),
                buy(SkofcraftItems.TOBACCO_DUST, 12, 12, 8)));
        level2.add(bundle(
                sell(SkofcraftItems.SKOFEE_SEEDS, 8, 1, 16, 8),
                sell(SkofcraftItems.SKOFEE, 1, 2, 12, 8),
                buy(SkofcraftItems.SKOFEE_BEANS_ROASTED, 12, 12, 8)));

        List<VillagerTrades.ItemListing> level3 = event.getTrades().get(3);
        level3.add(bundle(
                sell(SkofcraftItems.MANUAL_GRINDER, 1, 8, 6, 15),
                sell(SkofcraftItems.FLAVOR_BERRY, 2, 5, 8, 15),
                sell(SkofcraftItems.FLAVOR_CITRUS, 2, 5, 8, 15)));
        level3.add(bundle(
                sell(SkofcraftItems.FLAVOR_ICE_MINT, 2, 5, 8, 15),
                sell(SkofcraftItems.FLAVOR_COLA, 2, 5, 8, 15),
                sell(SkofcraftItems.CIGARILLO, 3, 4, 12, 15),
                sell(SkofcraftItems.CHEWING_TOBACCO, 2, 3, 12, 15)));
        level3.add(bundle(
                sell(SkofcraftItems.SKOFEE_MACHINE, 1, 12, 4, 15),
                sell(SkofcraftItems.SKOFEE_LATTE, 1, 3, 12, 15),
                buy(SkofcraftItems.SKOFEE_GROUND, 16, 12, 15)));

        List<VillagerTrades.ItemListing> level4 = event.getTrades().get(4);
        level4.add(bundle(
                sell(SkofcraftItems.SNUS_POUCH, 1, 2, 16, 20),
                sell(SkofcraftItems.EMPTY_SNUS_BOX, 2, 7, 6, 20),
                sell(SkofcraftItems.EMPTY_POUCH_BOX, 2, 6, 6, 20),
                buy(SkofcraftItems.NICOTINE_EXTRACT, 3, 12, 20)));
        level4.add(bundle(
                sell(SkofcraftItems.FLAVOR_LICORICE, 2, 6, 8, 20),
                sell(SkofcraftItems.FLAVOR_COFFEE, 2, 6, 8, 20),
                sell(SkofcraftItems.FLAVOR_NATURE, 2, 6, 8, 20),
                sell(SkofcraftItems.SNUFF, 1, 3, 12, 20),
                buyStrong(SkofcraftItems.SNUS_POUCH, 2, 3, 8, 20)));

        List<VillagerTrades.ItemListing> level5 = event.getTrades().get(5);
        level5.add(bundle(
                sell(SkofcraftItems.FILLED_SNUS_BOX, 1, 18, 4, 30),
                sell(SkofcraftItems.NICOTINE_POUCH, 1, 4, 12, 30),
                buy(SkofcraftItems.FILLED_SNUS_BOX, 1, 12, 4, 30),
                buy(SkofcraftItems.FILLED_POUCH_BOX, 1, 14, 4, 30),
                buy(SkofcraftItems.CIGARETTE_PACK, 1, 10, 4, 30)));
        level5.add(bundle(
                sell(SkofcraftItems.CIGAR, 2, 8, 6, 30),
                sell(SkofcraftItems.VAPE, 1, 14, 4, 30),
                sell(SkofcraftItems.EMPTY_VAPE, 1, 6, 8, 30),
                sell(SkofcraftItems.CHICHA, 1, 24, 2, 30),
                sell(SkofcraftItems.ADDICTOL, 1, 32, 2, 30),
                buyStrong(SkofcraftItems.SNUS_POUCH, 3, 6, 6, 30)));
    }

    @SubscribeEvent
    public static void onWandererTrades(WandererTradesEvent event) {
        if (!SkofcraftConfig.enableVillager) {
            return;
        }
        event.getGenericTrades().add(sell(SkofcraftItems.TOBACCO_SEEDS, 4, 1, 8, 1));
        event.getGenericTrades().add(sell(SkofcraftItems.SKOFEE_SEEDS, 4, 1, 8, 1));
        event.getRareTrades().add(sell(SkofcraftItems.ADDICTOL, 1, 20, 1, 5));
    }

    /** Hands every listing of a group to the villager; vanilla only asks one offer per listing. */
    private static VillagerTrades.ItemListing bundle(VillagerTrades.ItemListing... listings) {
        return (Entity trader, RandomSource random) -> {
            MerchantOffer last = null;
            for (VillagerTrades.ItemListing listing : listings) {
                MerchantOffer offer = listing.getOffer(trader, random);
                if (offer == null) {
                    continue;
                }
                if (last != null && trader instanceof Villager villager) {
                    villager.getOffers().add(last);
                }
                last = offer;
            }
            return last;
        };
    }

    private static VillagerTrades.ItemListing sell(Supplier<? extends Item> item, int itemCount, int emeraldCost, int maxUses, int xp) {
        return (trader, random) -> new MerchantOffer(
                new ItemStack(Items.EMERALD, emeraldCost),
                new ItemStack(item.get(), itemCount),
                maxUses, xp, 0.05F);
    }

    private static VillagerTrades.ItemListing buy(Supplier<? extends Item> item, int itemCount, int maxUses, int xp) {
        return buy(item, itemCount, 1, maxUses, xp);
    }

    private static VillagerTrades.ItemListing buy(Supplier<? extends Item> item, int itemCount, int emeraldReward, int maxUses, int xp) {
        return (trader, random) -> new MerchantOffer(
                new ItemStack(item.get(), itemCount),
                new ItemStack(Items.EMERALD, emeraldReward),
                maxUses, xp, 0.05F);
    }

    /** Buys a pouch of the given strength for emeralds. */
    private static VillagerTrades.ItemListing buyStrong(Supplier<? extends Item> item, int strength, int emeraldReward, int maxUses, int xp) {
        return (trader, random) -> {
            ItemStack cost = new ItemStack(item.get());
            cost.getOrCreateTag().putInt(STRENGTH_TAG, strength);
            return new MerchantOffer(cost, new ItemStack(Items.EMERALD, emeraldReward), maxUses, xp, 0.05F);
        };
    }

    private static VillagerTrades.ItemListing swap(Item costA, int countA, Item costB, int countB,
                                                   Supplier<? extends Item> result, int resultCount, int maxUses, int xp) {
        return (trader, random) -> new MerchantOffer(
                new ItemStack(costA, countA),
                new ItemStack(costB, countB),
                new ItemStack(result.get(), resultCount),
                maxUses, xp, 0.05F);
    }
}
