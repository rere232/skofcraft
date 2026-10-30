package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.entity.ApacheHelicopter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;

import java.util.List;

/** Places an Apache attack helicopter on the clicked block. */
public class ApacheItem extends Item {
    public ApacheItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof net.minecraft.server.level.ServerLevel)) {
            return InteractionResult.SUCCESS;
        }
        Direction face = context.getClickedFace();
        BlockPos pos = context.getClickedPos().relative(face);
        Player player = context.getPlayer();
        float yaw = player != null ? player.getYRot() : 0.0F;

        ApacheHelicopter helicopter = new ApacheHelicopter(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw);
        if (!level.noCollision(helicopter, helicopter.getBoundingBox())) {
            return InteractionResult.FAIL;
        }
        level.addFreshEntity(helicopter);
        if (player == null || !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.skofcraft.apache").withStyle(ChatFormatting.GRAY));
    }
}
