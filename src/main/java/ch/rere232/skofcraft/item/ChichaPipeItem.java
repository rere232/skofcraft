package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.blockentity.ChichaBlockEntity;
import ch.rere232.skofcraft.gums.GumSlotData;
import ch.rere232.skofcraft.gums.GumStatusEffects;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/** A hose taken from a chicha: puff on it while within range of the chicha it came from. */
public class ChichaPipeItem extends Item {
    private static final int USE_TICKS = 40;
    private static final int COOLDOWN_TICKS = 40;

    public ChichaPipeItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static ItemStack create(Level level, BlockPos pos, int tube) {
        ItemStack stack = new ItemStack(SkofcraftItems.CHICHA_PIPE.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("ChichaX", pos.getX());
        tag.putInt("ChichaY", pos.getY());
        tag.putInt("ChichaZ", pos.getZ());
        tag.putInt("Tube", tube);
        tag.putString("Dim", level.dimension().location().toString());
        return stack;
    }

    public static boolean matches(ItemStack stack, BlockPos pos, int tube) {
        if (!(stack.getItem() instanceof ChichaPipeItem) || !stack.hasTag()) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag.getInt("ChichaX") == pos.getX() && tag.getInt("ChichaY") == pos.getY()
                && tag.getInt("ChichaZ") == pos.getZ() && tag.getInt("Tube") == tube;
    }

    /** The chicha this hose belongs to, if the player still holds the hose and is in range. */
    @Nullable
    private static ChichaBlockEntity resolve(Level level, ItemStack stack, Player player) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.getString("Dim").equals(level.dimension().location().toString())) {
            return null;
        }
        BlockPos pos = new BlockPos(tag.getInt("ChichaX"), tag.getInt("ChichaY"), tag.getInt("ChichaZ"));
        if (!level.isLoaded(pos)) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ChichaBlockEntity chicha)) {
            return null;
        }
        if (!chicha.isUser(tag.getInt("Tube"), player.getUUID()) || !chicha.isInRange(player)) {
            return null;
        }
        return chicha;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.CUSTOM;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            ChichaBlockEntity chicha = resolve(level, stack, player);
            if (chicha == null) {
                return InteractionResultHolder.fail(stack);
            }
            if (chicha.getPuffs() <= 0) {
                player.displayClientMessage(Component.translatable("message.skofcraft.chicha_empty"), true);
                return InteractionResultHolder.fail(stack);
            }
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingTicks) {
        int elapsed = USE_TICKS - remainingTicks;
        if (!level.isClientSide && (elapsed == 4 || elapsed == 14 || elapsed == 24)) {
            level.playSound(null, entity.getX(), entity.getEyeY(), entity.getZ(), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.PLAYERS, 0.6F, 1.2F);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return stack;
        }
        ChichaBlockEntity chicha = resolve(level, stack, player);
        if (chicha == null || !chicha.consumePuff()) {
            return stack;
        }

        GumStatusEffects.applyChicha(player, chicha.getFlavor());
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
        }
        GumSlotData.onVapePuff(player, 0);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.CANDLE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 0.8F);
        if (level instanceof ServerLevel serverLevel) {
            Vec3 mouth = player.getEyePosition().add(player.getLookAngle().scale(0.4D));
            serverLevel.sendParticles(ParticleTypes.CLOUD, mouth.x, mouth.y - 0.2D, mouth.z, 16, 0.15D, 0.06D, 0.15D, 0.02D);
            Vec3 vase = Vec3.atCenterOf(chicha.getBlockPos());
            serverLevel.sendParticles(ParticleTypes.BUBBLE, vase.x, vase.y - 0.1D, vase.z, 8, 0.12D, 0.1D, 0.12D, 0.02D);
        }
        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player) || level.getGameTime() % 10 != 0) {
            return;
        }
        if (resolve(level, stack, player) == null) {
            stack.setCount(0);
            player.displayClientMessage(Component.translatable("message.skofcraft.chicha_far"), true);
        }
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.skofcraft.chicha_pipe", ChichaBlockEntity.RANGE));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                if (entity.isUsingItem() && entity.getUsedItemHand() == hand) {
                    return HumanoidModel.ArmPose.TOOT_HORN;
                }
                return null;
            }

            @Override
            public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                                   float partialTick, float equipProcess, float swingProcess) {
                if (!player.isUsingItem() || player.getUseItem().getItem() != stack.getItem()) {
                    return false;
                }
                float elapsed = USE_TICKS - (player.getUseItemRemainingTicks() - partialTick);
                float t = Mth.clamp(elapsed / USE_TICKS, 0.0F, 1.0F);
                float raise = t < 0.2F ? ease(t / 0.2F) : (t > 0.85F ? ease((1.0F - t) / 0.15F) : 1.0F);
                float draw = (t > 0.25F && t < 0.8F) ? raise * (Mth.sin(elapsed * 0.6F) * 0.5F + 0.5F) : 0.0F;
                float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;

                pose.translate(side * Mth.lerp(raise, 0.56F, 0.12F),
                        Mth.lerp(raise, -0.52F, -0.26F) + draw * 0.012F,
                        Mth.lerp(raise, -0.72F, -0.48F));
                pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(raise, 0.0F, -20.0F)));
                pose.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(raise, 0.0F, -35.0F)));
                pose.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(raise, 0.0F, 25.0F)));
                return true;
            }
        });
    }

    private static float ease(float x) {
        return x * x * (3.0F - 2.0F * x);
    }
}
