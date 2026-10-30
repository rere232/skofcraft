package ch.rere232.skofcraft.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Intermediate mixture that carries the flavor and strength of the pouch it will become. */
public class PouchMixItem extends Item {
    public PouchMixItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        GumConsumableItem.addTraitTooltip(stack, tooltip);
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
