package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/** An Amazooon box with the whole Amazon femboy fit inside. Right-click to unbox it. */
public class OutfitBoxItem extends Item {
    private final Supplier<List<RegistryObject<Item>>> contents;
    private final String tooltipKey;

    /** The six pieces needed for the outfit advancement. */
    public OutfitBoxItem(Properties properties) {
        this(properties, () -> List.of(SkofcraftItems.FACE_MASK, SkofcraftItems.CHOKER, SkofcraftItems.CROP_TOP, SkofcraftItems.HAND_WARMERS,
                SkofcraftItems.SKIRT, SkofcraftItems.THIGH_HIGHS), "tooltip.skofcraft.outfit_box");
    }

    public OutfitBoxItem(Properties properties, Supplier<List<RegistryObject<Item>>> contents, String tooltipKey) {
        super(properties);
        this.contents = contents;
        this.tooltipKey = tooltipKey;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack box = player.getItemInHand(hand);
        if (!level.isClientSide) {
            for (RegistryObject<Item> piece : contents.get()) {
                ItemStack stack = new ItemStack(piece.get());
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SkofcraftSounds.CLOTHES_RUSTLE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("message.skofcraft.outfit_box_opened"), true);
            if (!player.getAbilities().instabuild) {
                box.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(box, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
        for (RegistryObject<Item> piece : contents.get()) {
            tooltip.add(Component.literal(" - ").append(piece.get().getDescription()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
