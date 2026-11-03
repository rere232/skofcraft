package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.registry.SkofcraftItems;
import ch.rere232.skofcraft.registry.SkofcraftPastel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * Runs the shops in Pastel Paradise and only takes Heart Tokens. This is the slow, certain road to The Rightful Fit, for
 * anyone the chests have let down.
 */
public class BoutiqueKeeper extends KingdomNoble {
    private static final int COSMETIC_PRICE = 6;
    private static final int STOCK = 8;
    private static final int FIT_STOCK = 1;
    private static final int XP = 5;
    private static final float PRICE_MULTIPLIER = 0.0F;

    public BoutiqueKeeper(EntityType<? extends BoutiqueKeeper> type, Level level) {
        super(type, level);
    }

    /** Stays by the till, where it can be found, and away from the edge of the island. */
    @Override
    protected void addWanderGoals() {}

    private static ItemStack hearts(int count) {
        return new ItemStack(SkofcraftPastel.HEART_TOKEN.get(), count);
    }

    private static MerchantOffer offer(int price, Item item, int count) {
        return new MerchantOffer(hearts(price), new ItemStack(item, count), STOCK, XP, PRICE_MULTIPLIER);
    }

    @Override
    protected void updateTrades() {
        List<RegistryObject<Item>> clothes = List.of(SkofcraftPastel.PASTEL_SKIRT, SkofcraftPastel.STRIPED_THIGH_HIGHS,
                SkofcraftPastel.PASTEL_CROP_TOP, SkofcraftPastel.HEART_CHOKER, SkofcraftPastel.BUNNY_EARS, SkofcraftPastel.FOX_TAIL);
        for (RegistryObject<Item> piece : clothes) {
            getOffers().add(offer(COSMETIC_PRICE, piece.get(), 1));
        }
        getOffers().add(offer(2, SkofcraftPastel.COTTON_PUFF.get(), 2));
        getOffers().add(offer(8, SkofcraftItems.BLAHAJ.get(), 1));
        getOffers().add(offer(16, SkofcraftPastel.THERAPY_COUCH.get().asItem(), 1));
        getOffers().add(offer(24, SkofcraftPastel.HEART_ALTAR.get().asItem(), 1));
        getOffers().add(new MerchantOffer(new ItemStack(Items.CAKE, 1), hearts(1), STOCK, XP, PRICE_MULTIPLIER));
        getOffers().add(new MerchantOffer(new ItemStack(Items.GOLD_INGOT, 4), hearts(1), STOCK, XP, PRICE_MULTIPLIER));
        getOffers().add(new MerchantOffer(hearts(64), hearts(64), new ItemStack(SkofcraftPastel.RIGHTFUL_FIT_BOX.get()), FIT_STOCK, XP, PRICE_MULTIPLIER));
    }
}
