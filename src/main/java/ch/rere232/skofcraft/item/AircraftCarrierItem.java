package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.carrier.CarrierBlocks;
import ch.rere232.skofcraft.carrier.CarrierLayout;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Right-click open water to launch a carrier: it is built ahead of you, stern first, bow pointing where you look. */
public class AircraftCarrierItem extends Item {
    public AircraftCarrierItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK || !level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        BlockPos surface = hit.getBlockPos();
        while (level.getFluidState(surface.above()).is(FluidTags.WATER)) {
            surface = surface.above();
        }
        Direction facing = player.getDirection();
        BlockPos origin = surface.relative(facing, CarrierLayout.STERN_Z + 3).below(CarrierLayout.WATERLINE);
        if (!CarrierBlocks.canDeploy(level, origin, facing)) {
            player.displayClientMessage(Component.translatable("message.skofcraft.carrier_no_room"), true);
            return InteractionResultHolder.fail(stack);
        }

        CarrierBlocks.fromLayout().place(level, origin, facing);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SkofcraftSounds.SHIP_HORN.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.skofcraft.carrier_deployed"), true);
        SkofcraftAdvancements.award(player, SkofcraftAdvancements.NUCLEAR_NAVY);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.skofcraft.aircraft_carrier").withStyle(ChatFormatting.GRAY));
    }
}
