package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.space.SatelliteType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A satellite waiting for a ride: load it onto a Launch Pad to put it in orbit. */
public class SatelliteItem extends Item {
    public final SatelliteType type;

    public SatelliteItem(Properties properties, SatelliteType type) {
        super(properties);
        this.type = type;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(type.key() + ".tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.skofcraft.satellite_launch").withStyle(ChatFormatting.DARK_GRAY));
    }
}
