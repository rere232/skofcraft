package ch.rere232.skofcraft.villager;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.config.SkofcraftConfig;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import ch.rere232.skofcraft.registry.SkofcraftVillagers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.util.RandomSource;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SkofcraftVillagerTrades {
    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (!SkofcraftConfig.enableVillager || event.getType() != SkofcraftVillagers.TOBACCONIST.get()) {
            return;
        }

        List<VillagerTrades.ItemListing> level1 = event.getTrades().get(1);
        level1.add(sellForEmeralds(SkofcraftItems.TOBACCO_SEEDS.get(), 1, 8, 16, 2));
        level1.add(buyForEmeralds(SkofcraftItems.TOBACCO_LEAF_FRESH.get(), 12, 16, 2));

        List<VillagerTrades.ItemListing> level2 = event.getTrades().get(2);
        level2.add(sellForEmeralds(SkofcraftItems.MANUAL_DRYER.get(), 6, 1, 6, 10));
        level2.add(buyForEmeralds(SkofcraftItems.TOBACCO_LEAF_DRY.get(), 16, 12, 10));

        List<VillagerTrades.ItemListing> level3 = event.getTrades().get(3);
        level3.add(sellForEmeralds(SkofcraftItems.EMPTY_POUCH.get(), 2, 16, 12, 15));
        level3.add(sellForEmeralds(SkofcraftItems.FLAVOR_MINT.get(), 4, 2, 8, 15));

        List<VillagerTrades.ItemListing> level4 = event.getTrades().get(4);
        level4.add(sellForEmeralds(SkofcraftItems.SNUS_POUCH.get(), 5, 4, 8, 20));
        level4.add(sellForEmeralds(SkofcraftItems.EMPTY_SNUS_BOX.get(), 7, 2, 6, 20));

        List<VillagerTrades.ItemListing> level5 = event.getTrades().get(5);
        level5.add(sellForEmeralds(SkofcraftItems.FILLED_SNUS_BOX.get(), 18, 1, 4, 30));
        level5.add(sellForEmeralds(SkofcraftItems.NICOTINE_POUCH.get(), 9, 2, 6, 30));
    }

    private static VillagerTrades.ItemListing sellForEmeralds(Item item, int emeraldCost, int itemCount, int maxUses, int villagerXp) {
        return (Entity trader, RandomSource random) -> new MerchantOffer(
                new ItemStack(Items.EMERALD, emeraldCost),
                new ItemStack(item, itemCount),
                maxUses,
                villagerXp,
                0.05F
        );
    }

    private static VillagerTrades.ItemListing buyForEmeralds(Item item, int itemCount, int maxUses, int villagerXp) {
        return (Entity trader, RandomSource random) -> new MerchantOffer(
                new ItemStack(item, itemCount),
                new ItemStack(Items.EMERALD),
                maxUses,
                villagerXp,
                0.05F
        );
    }
}
