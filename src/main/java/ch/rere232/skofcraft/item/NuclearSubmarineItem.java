package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.entity.NuclearSubmarine;
import ch.rere232.skofcraft.registry.SkofcraftEntities;
import ch.rere232.skofcraft.registry.SkofcraftSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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

/** Right-click open water to launch the boat ahead of you, stern first, with the bow pointing where you look. */
public class NuclearSubmarineItem extends Item {
    private static final double STERN_CLEARANCE = 4.0D;

    public NuclearSubmarineItem(Properties properties) {
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
        float yaw = player.getYRot();
        double rad = Math.toRadians(yaw);
        double ahead = NuclearSubmarine.HALF_LENGTH + STERN_CLEARANCE;
        double x = surface.getX() + 0.5D - Math.sin(rad) * ahead;
        double y = surface.getY() + 1.0D - NuclearSubmarine.WATERLINE;
        double z = surface.getZ() + 0.5D + Math.cos(rad) * ahead;
        if (!NuclearSubmarine.canFloatAt(level, x, y, z, yaw)) {
            player.displayClientMessage(Component.translatable("message.skofcraft.submarine_no_room"), true);
            return InteractionResultHolder.fail(stack);
        }

        NuclearSubmarine boat = SkofcraftEntities.NUCLEAR_SUBMARINE.get().create(level);
        if (boat == null) {
            return InteractionResultHolder.fail(stack);
        }
        boat.place(x, y, z, yaw);
        level.addFreshEntity(boat);
        level.playSound(null, x, y + NuclearSubmarine.DECK_HEIGHT, z, SkofcraftSounds.SHIP_HORN.get(), SoundSource.NEUTRAL, 2.0F, 0.8F);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.skofcraft.nuclear_submarine").withStyle(ChatFormatting.GRAY));
    }
}
