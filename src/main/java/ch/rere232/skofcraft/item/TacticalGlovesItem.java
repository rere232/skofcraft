package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.List;
import java.util.UUID;

/**
 * Tactical gloves, the fifth piece of the tactical armor: armored knuckles worn in the Curios hands slot. They hit
 * harder and swing faster, and the full set's bonus needs them too.
 */
public class TacticalGlovesItem extends ClothingItem {
    public TacticalGlovesItem(Properties properties) {
        super(properties);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        return ImmutableMultimap.of(
                Attributes.ATTACK_DAMAGE, new AttributeModifier(uuid, "Tactical gloves damage", 3.0D, AttributeModifier.Operation.ADDITION),
                Attributes.ATTACK_SPEED, new AttributeModifier(uuid, "Tactical gloves speed", 0.15D, AttributeModifier.Operation.MULTIPLY_BASE),
                Attributes.ARMOR, new AttributeModifier(uuid, "Tactical gloves armor", 2.0D, AttributeModifier.Operation.ADDITION));
    }

    @Override
    public ICurio.SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
        return new ICurio.SoundInfo(SkofcraftSounds.ARMOR_EQUIP.get(), 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.skofcraft.tactical_gloves").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.skofcraft.tactical_set").withStyle(ChatFormatting.DARK_AQUA));
    }
}
