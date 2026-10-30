package ch.rere232.skofcraft.delivery;

import ch.rere232.skofcraft.registry.SkofcraftItems;
import ch.rere232.skofcraft.entity.DeliveryPlane;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class DeliveryService {
    private DeliveryService() {}

    /** Charges the player in diamonds and sends a plane to drop the order on them. */
    public static void order(ServerPlayer player, int productIndex) {
        if (productIndex < 0 || productIndex >= AmazooonCatalog.PRODUCTS.size() || !player.isAlive()) {
            return;
        }
        Inventory inventory = player.getInventory();
        if (!inventory.hasAnyMatching(stack -> stack.is(SkofcraftItems.PHONE.get()))) {
            return;
        }

        AmazooonCatalog.Product product = AmazooonCatalog.PRODUCTS.get(productIndex);
        if (!player.getAbilities().instabuild && countDiamonds(inventory) < product.price()) {
            player.displayClientMessage(Component.translatable("phone.skofcraft.amazooon.no_funds"), true);
            return;
        }
        if (!player.getAbilities().instabuild) {
            removeDiamonds(inventory, product.price());
        }

        DeliveryPlane.dispatch(player, product.stack());
        player.displayClientMessage(Component.translatable("phone.skofcraft.amazooon.on_the_way"), false);
    }

    public static int countDiamonds(Inventory inventory) {
        int total = 0;
        for (ItemStack stack : inventory.items) {
            if (stack.is(Items.DIAMOND)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static void removeDiamonds(Inventory inventory, int amount) {
        for (ItemStack stack : inventory.items) {
            if (amount <= 0) {
                return;
            }
            if (stack.is(Items.DIAMOND)) {
                int taken = Math.min(amount, stack.getCount());
                stack.shrink(taken);
                amount -= taken;
            }
        }
    }
}
