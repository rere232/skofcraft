package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.entity.AircraftFuel;
import ch.rere232.skofcraft.entity.Tank;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/** Places a tank on the clicked block, facing the way the player is looking. */
public class TankItem extends Item {
    private final Supplier<? extends EntityType<? extends Tank>> type;
    private final String tooltipKey;
    private final int maxFuel;

    public TankItem(Properties properties, Supplier<? extends EntityType<? extends Tank>> type, String tooltipKey, int maxFuel) {
        super(properties);
        this.type = type;
        this.tooltipKey = tooltipKey;
        this.maxFuel = maxFuel;
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

        Tank tank = type.get().create(level);
        if (tank == null) {
            return InteractionResult.FAIL;
        }
        tank.place(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw);
        if (!level.noCollision(tank, tank.getBoundingBox())) {
            return InteractionResult.FAIL;
        }
        tank.setFuel(AircraftFuel.fuelOf(context.getItemInHand()));
        level.addFreshEntity(tank);
        if (player == null || !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
        tooltip.add(AircraftFuel.tooltip(stack, maxFuel));
    }
}
