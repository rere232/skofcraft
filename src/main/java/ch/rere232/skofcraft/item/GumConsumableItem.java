package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.gums.GumSlotData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

public class GumConsumableItem extends Item {
    private final Supplier<Integer> durationMinutesSupplier;

    public GumConsumableItem(Properties properties, Supplier<Integer> durationMinutesSupplier) {
        super(properties);
        this.durationMinutesSupplier = durationMinutesSupplier;
    }

    public int getDurationMinutes() {
        return Math.max(1, durationMinutesSupplier.get());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            boolean inserted = GumSlotData.tryInsert(player, stack);
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
