package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.hr.MandatoryHr;
import ch.rere232.skofcraft.registry.SkofcraftSounds;
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

import javax.annotation.Nullable;
import java.util.List;

/** Signing it moves the player's HR limit from 3 incidents to 30, for good. Used up by signing. */
public class KnowWhatImDoingCertificateItem extends Item {
    public KnowWhatImDoingCertificateItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!MandatoryHr.certify(player)) {
            player.displayClientMessage(Component.translatable("message.skofcraft.hr_already_certified"), true);
            return InteractionResultHolder.fail(stack);
        }
        stack.shrink(1);
        level.playSound(null, player.blockPosition(), SkofcraftSounds.HR_CHIME.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
        player.displayClientMessage(Component.translatable("message.skofcraft.hr_certified",
                MandatoryHr.CERTIFIED_CRIMES_BEFORE_TRAINING), true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.skofcraft.know_what_im_doing_certificate.0").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.skofcraft.know_what_im_doing_certificate.1",
                MandatoryHr.CERTIFIED_CRIMES_BEFORE_TRAINING, MandatoryHr.CRIMES_BEFORE_TRAINING).withStyle(ChatFormatting.GRAY));
    }
}
