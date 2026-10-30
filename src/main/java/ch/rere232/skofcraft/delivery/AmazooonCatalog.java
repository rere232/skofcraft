package ch.rere232.skofcraft.delivery;

import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

/** Everything Amazooon sells, shared by the phone app and the server-side order check. */
public final class AmazooonCatalog {
    public record Product(Supplier<? extends Item> item, int count, int price) {
        public ItemStack stack() {
            return new ItemStack(item.get(), count);
        }
    }

    public static final List<Product> PRODUCTS = List.of(
            new Product(SkofcraftItems.CIGARETTE_PACK, 1, 2),
            new Product(SkofcraftItems.CIGARILLO, 4, 2),
            new Product(SkofcraftItems.CIGAR, 2, 3),
            new Product(SkofcraftItems.SNUFF, 4, 2),
            new Product(SkofcraftItems.CHEWING_TOBACCO, 4, 2),
            new Product(SkofcraftItems.FILLED_SNUS_BOX, 1, 4),
            new Product(SkofcraftItems.FILLED_POUCH_BOX, 1, 4),
            new Product(SkofcraftItems.VAPE, 1, 5),
            new Product(SkofcraftItems.CHICHA, 1, 8),
            new Product(SkofcraftItems.ADDICTOL, 1, 10)
    );

    private AmazooonCatalog() {}
}
