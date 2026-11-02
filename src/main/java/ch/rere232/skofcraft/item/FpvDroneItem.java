package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.entity.FpvDrone;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Throws a drone into the air in front of you and puts you on its camera. */
public class FpvDroneItem extends Item {
    private static final double LAUNCH_DISTANCE = 0.8D;
    private static final double LAUNCH_SPEED = 0.25D;

    public FpvDroneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        Component problem = FpvDrone.whyNotPilot(player);
        if (problem != null) {
            player.displayClientMessage(problem, true);
            return InteractionResultHolder.fail(stack);
        }
        Vec3 look = player.getLookAngle();
        Vec3 launch = player.getEyePosition().add(look.scale(LAUNCH_DISTANCE));
        FpvDrone drone = new FpvDrone(level, launch.x, launch.y, launch.z, player.getYRot());
        if (!level.noCollision(drone, drone.getBoundingBox())) {
            // A wall in the face: straight up from the shoulders instead
            drone.setPos(player.getX(), player.getBoundingBox().maxY + 0.1D, player.getZ());
            if (!level.noCollision(drone, drone.getBoundingBox())) {
                player.displayClientMessage(Component.translatable("message.skofcraft.fpv_no_room"), true);
                return InteractionResultHolder.fail(stack);
            }
        }
        drone.setXRot(player.getXRot());
        drone.setDeltaMovement(look.scale(LAUNCH_SPEED).add(0.0D, LAUNCH_SPEED, 0.0D));
        level.addFreshEntity(drone);
        drone.link(serverPlayer);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.skofcraft.fpv_drone").withStyle(ChatFormatting.GRAY));
    }
}
