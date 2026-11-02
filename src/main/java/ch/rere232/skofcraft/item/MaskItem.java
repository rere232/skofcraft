package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.SkofcraftMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;
import java.util.Locale;

/**
 * A black face mask worn in the Curios head slot, with a cute face printed on it. One item, four styles:
 * sneak-right-click to cycle them, right-click to put it on. The style lives in the stack's NBT.
 */
public class MaskItem extends ClothingItem {
    public enum Style {
        CAT, TONGUE, FANGS, BLUSH;

        public final String id = name().toLowerCase(Locale.ROOT);
        public final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "textures/entity/mask_" + id + ".png");

        public Component displayName() {
            return Component.translatable("item.skofcraft.face_mask.style." + id);
        }
    }

    private static final String STYLE = "Style";

    public MaskItem(Properties properties) {
        super(properties);
    }

    public static Style style(ItemStack stack) {
        Style[] styles = Style.values();
        // Read without creating a tag: this also runs every frame for rendering
        int id = stack.getTag() == null ? 0 : stack.getTag().getInt(STYLE);
        return styles[Math.floorMod(id, styles.length)];
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }
        if (!level.isClientSide) {
            int next = (style(stack).ordinal() + 1) % Style.values().length;
            stack.getOrCreateTag().putInt(STYLE, next);
            player.displayClientMessage(Component.translatable("message.skofcraft.face_mask_style", style(stack).displayName())
                    .withStyle(ChatFormatting.GRAY), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SkofcraftSounds.CLOTHES_RUSTLE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Sneaking is for changing the style, so only a plain right-click puts the mask on. */
    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return !slotContext.entity().isShiftKeyDown();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.skofcraft.face_mask.style", style(stack).displayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.skofcraft.face_mask").withStyle(ChatFormatting.GRAY));
    }
}
