package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.block.CarrierHelmBlock;
import ch.rere232.skofcraft.carrier.CarrierBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Use on an anchored carrier's helm: rebuilds missing hull, deck and fittings and pumps out flooded compartments.
 * Each block rebuilt costs one point of durability; pumping is free.
 */
public class DamageControlKitItem extends Item {
    /** How much of the ship must still stand for the helm to find the rest of it. */
    private static final float MIN_INTEGRITY = 0.25F;

    public DamageControlKitItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockState state = level.getBlockState(context.getClickedPos());
        if (!(state.getBlock() instanceof CarrierHelmBlock)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Direction facing = state.getValue(CarrierHelmBlock.FACING);
        BlockPos origin = CarrierHelmBlock.originOf(context.getClickedPos(), facing);
        if (CarrierBlocks.integrity(level, origin, facing) < MIN_INTEGRITY) {
            message(player, Component.translatable("message.skofcraft.carrier_not_a_ship"));
            return InteractionResult.FAIL;
        }

        boolean creative = player != null && player.getAbilities().instabuild;
        int budget = creative ? Integer.MAX_VALUE : stack.getMaxDamage() - stack.getDamageValue();
        CarrierBlocks.RepairResult result = CarrierBlocks.repair(level, origin, facing, budget);
        if (result.rebuilt() == 0 && result.pumped() == 0) {
            message(player, Component.translatable("message.skofcraft.carrier_repair_none"));
            return InteractionResult.CONSUME;
        }
        if (!creative && result.rebuilt() > 0 && player != null) {
            stack.hurtAndBreak(result.rebuilt(), player, p -> p.broadcastBreakEvent(context.getHand()));
        }

        BlockPos helm = context.getClickedPos();
        level.playSound(null, helm, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, helm.getX() + 0.5D, helm.getY() + 1.0D, helm.getZ() + 0.5D,
                    12, 0.4D, 0.3D, 0.4D, 0.0D);
        }
        message(player, result.remaining() > 0
                ? Component.translatable("message.skofcraft.carrier_repair_partial", result.rebuilt(), result.pumped(), result.remaining())
                : Component.translatable("message.skofcraft.carrier_repaired", result.rebuilt(), result.pumped()));
        return InteractionResult.CONSUME;
    }

    private static void message(@Nullable Player player, Component text) {
        if (player != null) {
            player.displayClientMessage(text, true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.skofcraft.damage_control_kit").withStyle(ChatFormatting.GRAY));
    }
}
