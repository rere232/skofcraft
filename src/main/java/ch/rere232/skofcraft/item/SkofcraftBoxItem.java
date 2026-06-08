package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.config.SkofcraftConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

public class SkofcraftBoxItem extends Item {
    private static final String TAG_USES_LEFT = "UsesLeft";

    private final Supplier<Item> unitItem;
    private final Supplier<Item> emptyBoxItem;

    public SkofcraftBoxItem(Properties properties, Supplier<Item> unitItem, Supplier<Item> emptyBoxItem) {
        super(properties.stacksTo(1));
        this.unitItem = unitItem;
        this.emptyBoxItem = emptyBoxItem;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack boxStack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            int usesLeft = getUsesLeft(boxStack);
            if (usesLeft <= 0) {
                player.setItemInHand(hand, new ItemStack(emptyBoxItem.get()));
                return InteractionResultHolder.success(player.getItemInHand(hand));
            }

            ItemStack singleUnit = new ItemStack(unitItem.get());
            if (!player.addItem(singleUnit)) {
                player.drop(singleUnit, false);
            }

            usesLeft -= 1;
            if (usesLeft <= 0) {
                player.setItemInHand(hand, new ItemStack(emptyBoxItem.get()));
            } else {
                setUsesLeft(boxStack, usesLeft);
            }
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        int capacity = SkofcraftConfig.boxCapacity;
        int usesLeft = getUsesLeft(stack);
        tooltip.add(Component.translatable("tooltip.skofcraft.remaining", usesLeft, capacity));
    }

    private int getUsesLeft(ItemStack stack) {
        int capacity = SkofcraftConfig.boxCapacity;
        if (capacity <= 0) {
            capacity = 20;
        }
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(TAG_USES_LEFT)) {
            tag.putInt(TAG_USES_LEFT, capacity);
            return capacity;
        }
        return Math.min(tag.getInt(TAG_USES_LEFT), capacity);
    }

    private void setUsesLeft(ItemStack stack, int usesLeft) {
        stack.getOrCreateTag().putInt(TAG_USES_LEFT, Math.max(usesLeft, 0));
    }
}
