package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.entity.F14Tomcat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/** Places an F-14 Tomcat on the clicked block, nose toward where the player looks. */
public class F14Item extends Item {
    public F14Item(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Player player = context.getPlayer();
        float yaw = player != null ? player.getYRot() : 0.0F;

        F14Tomcat jet = new F14Tomcat(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw);
        if (!level.noCollision(jet, jet.getBoundingBox())) {
            return InteractionResult.FAIL;
        }
        level.addFreshEntity(jet);
        if (player == null || !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.skofcraft.f14").withStyle(ChatFormatting.GRAY));
    }
}
