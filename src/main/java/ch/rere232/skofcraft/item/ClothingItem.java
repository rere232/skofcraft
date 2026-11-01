package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * A piece of clothing worn in a Curios slot: the Face Mask and Cat Ears (head), Choker (necklace), Crop Top (body),
 * Hand Warmers (hands), Skirt (belt), Thigh Highs (legs) and Cat Tail (back).
 * Which slot each goes in is set by the curios item tags; {@link ch.rere232.skofcraft.client.ClothingRenderer} draws
 * them on the wearer. Right-click to put one on.
 */
public class ClothingItem extends Item implements ICurioItem {
    private static final int OUTFIT_CHECK_TICKS = 20;

    public ClothingItem(Properties properties) {
        super(properties);
    }

    /** The whole set; wearing all of it at once earns the outfit advancement. */
    private static List<RegistryObject<Item>> outfit() {
        return List.of(SkofcraftItems.FACE_MASK, SkofcraftItems.CHOKER, SkofcraftItems.CROP_TOP, SkofcraftItems.HAND_WARMERS,
                SkofcraftItems.SKIRT, SkofcraftItems.THIGH_HIGHS);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        // Only the choker checks, so the five pieces do not all run the same check
        if (!(slotContext.entity() instanceof ServerPlayer player) || this != SkofcraftItems.CHOKER.get()
                || player.tickCount % OUTFIT_CHECK_TICKS != 0) {
            return;
        }
        boolean complete = CuriosApi.getCuriosInventory(player)
                .map(curios -> outfit().stream().allMatch(piece -> curios.isEquipped(piece.get())))
                .orElse(false);
        if (complete) {
            SkofcraftAdvancements.award(player, SkofcraftAdvancements.FULL_OUTFIT);
        }
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public ICurio.SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
        return new ICurio.SoundInfo(SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 1.2F);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // item.skofcraft.<name> -> tooltip.skofcraft.<name>
        tooltip.add(Component.translatable("tooltip" + getDescriptionId().substring("item".length())).withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
