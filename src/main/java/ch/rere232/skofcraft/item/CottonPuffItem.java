package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** How you get between floating islands: a soft shove the way you are looking, and a slow way down. */
public class CottonPuffItem extends Item {
    private static final double PUSH = 1.4D;
    private static final double LIFT = 0.6D;
    private static final int GLIDE_TICKS = 120;
    private static final int COOLDOWN_TICKS = 20;

    public CottonPuffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Vec3 look = player.getLookAngle();
        // Moves on both sides, so the client does not have to wait for the server to agree
        player.setDeltaMovement(look.x * PUSH, Math.max(look.y * PUSH, 0.0D) + LIFT, look.z * PUSH);
        player.resetFallDistance();
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        if (!level.isClientSide) {
            player.hurtMarked = true;
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, GLIDE_TICKS, 0, true, false, true));
            level.playSound(null, player.blockPosition(), SkofcraftSounds.PLUSH_SOFT.get(), SoundSource.PLAYERS, 1.0F, 1.3F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.skofcraft.cotton_puff").withStyle(ChatFormatting.GRAY));
    }
}
