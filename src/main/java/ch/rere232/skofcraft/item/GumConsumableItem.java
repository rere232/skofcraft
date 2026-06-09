package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.gums.GumSlotData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

public class GumConsumableItem extends Item {
    public static final String TAG_FLAVOR = "SkofcraftFlavor";

    private final Supplier<Integer> durationMinutesSupplier;

    public GumConsumableItem(Properties properties, Supplier<Integer> durationMinutesSupplier) {
        super(properties);
        this.durationMinutesSupplier = durationMinutesSupplier;
    }

    public int getDurationMinutes() {
        return Math.max(1, durationMinutesSupplier.get());
    }

    public static void setFlavor(ItemStack pouch, ItemStack flavorItem) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(flavorItem.getItem());
        if (key != null) {
            pouch.getOrCreateTag().putString(TAG_FLAVOR, key.toString());
        }
    }

    public static String getFlavorId(ItemStack stack) {
        if (!stack.hasTag()) {
            return "";
        }
        return stack.getTag().getString(TAG_FLAVOR);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        String flavorId = getFlavorId(stack);
        if (!flavorId.isEmpty()) {
            tooltip.add(Component.literal("Flavor: " + flavorId.replace("skofcraft:flavor_", "").replace('_', ' ')));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean inserted = GumSlotData.tryInsert(player, stack);
        if (!level.isClientSide) {
            if (inserted && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (!inserted) {
                player.displayClientMessage(Component.translatable("message.skofcraft.gums_full"), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
