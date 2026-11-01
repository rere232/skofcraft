package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.entity.OrbitalRod;
import ch.rere232.skofcraft.space.IcbmTargets;
import ch.rere232.skofcraft.space.OrbitData;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Laser designator for strike satellites: right-click to mark the block you are looking at, up to
 * {@link #RANGE} blocks away. A tungsten rod falls on it a few seconds later. Each satellite then needs
 * {@link OrbitData#STRIKE_COOLDOWN} ticks to line up again.
 */
public class OrbitalDesignatorItem extends Item {
    public static final double RANGE = 512.0D;
    public static final double ICBM_MARK_RANGE = 1024.0D;

    public OrbitalDesignatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        player.getCooldowns().addCooldown(this, 20);
        if (player.isShiftKeyDown()) {
            return markIcbmTarget(player, stack);
        }
        OrbitData orbit = OrbitData.get(serverLevel);
        List<OrbitData.Satellite> usable = orbit.strikeSatellitesFor(serverPlayer);
        if (usable.isEmpty()) {
            message(player, Component.translatable("message.skofcraft.strike_no_satellite").withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }
        long now = level.getGameTime();
        OrbitData.Satellite satellite = orbit.readyStrike(serverPlayer, now);
        if (satellite == null) {
            long wait = Long.MAX_VALUE;
            for (OrbitData.Satellite candidate : usable) {
                wait = Math.min(wait, candidate.readyAt() - now);
            }
            message(player, Component.translatable("message.skofcraft.strike_recharging", (wait + 19) / 20).withStyle(ChatFormatting.GOLD));
            return InteractionResultHolder.fail(stack);
        }
        HitResult hit = player.pick(RANGE, 1.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            message(player, Component.translatable("message.skofcraft.strike_no_target").withStyle(ChatFormatting.GOLD));
            return InteractionResultHolder.fail(stack);
        }
        Vec3 target = hit.getLocation();
        orbit.fire(satellite, now);
        OrbitalRod.call(serverLevel, target, player);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.6F);
        message(player, Component.translatable("message.skofcraft.strike_inbound",
                (int) Math.round(target.distanceTo(player.position())), OrbitalRod.WARNING_TICKS / 20).withStyle(ChatFormatting.RED));
        return InteractionResultHolder.consume(stack);
    }

    /** Sneak-use: mark the block in view as the Giga ICBM target, no satellite needed. */
    private InteractionResultHolder<ItemStack> markIcbmTarget(Player player, ItemStack stack) {
        HitResult hit = player.pick(ICBM_MARK_RANGE, 1.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            message(player, Component.translatable("message.skofcraft.strike_no_target").withStyle(ChatFormatting.GOLD));
            return InteractionResultHolder.fail(stack);
        }
        BlockPos pos = BlockPos.containing(hit.getLocation());
        IcbmTargets.set(player, pos.getX(), pos.getZ());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 1.0F, 0.6F);
        message(player, Component.translatable("message.skofcraft.icbm_marked", pos.getX(), pos.getZ()).withStyle(ChatFormatting.LIGHT_PURPLE));
        return InteractionResultHolder.consume(stack);
    }

    private static void message(Player player, Component text) {
        player.displayClientMessage(text, true);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.skofcraft.orbital_designator").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.skofcraft.orbital_designator.icbm").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
