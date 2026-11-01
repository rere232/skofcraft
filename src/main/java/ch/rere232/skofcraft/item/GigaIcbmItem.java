package ch.rere232.skofcraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Giga ICBM (and the creative Doomsday Missile, which is the same missile with a bigger yield), loaded onto a
 * Launch Pad like a satellite but flown to a target instead of into orbit.
 */
public class GigaIcbmItem extends Item {
    /** Blast size relative to the Giga ICBM. */
    public final float yieldScale;

    public GigaIcbmItem(Properties properties, float yieldScale) {
        super(properties);
        this.yieldScale = yieldScale;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // item.skofcraft.<name> -> tooltip.skofcraft.<name>
        tooltip.add(Component.translatable("tooltip" + getDescriptionId().substring("item".length())).withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.skofcraft.giga_icbm.how").withStyle(ChatFormatting.GRAY));
    }
}
