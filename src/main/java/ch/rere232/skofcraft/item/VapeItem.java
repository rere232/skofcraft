package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.config.SkofcraftConfig;
import ch.rere232.skofcraft.gums.GumSlotData;
import ch.rere232.skofcraft.gums.GumStatusEffects;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
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
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

/** Filled vape with a limited number of puffs; becomes an empty vape when used up. */
public class VapeItem extends Item {
    public static final String TAG_USES = "UsesLeft";
    private static final int USE_TICKS = 28;
    private static final int COOLDOWN_TICKS = 20;

    public VapeItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static int capacity() {
        return Math.max(1, SkofcraftConfig.vapeUses);
    }

    public static int getUsesLeft(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_USES)) {
            return capacity();
        }
        return Math.max(0, Math.min(capacity(), tag.getInt(TAG_USES)));
    }

    private static void setUsesLeft(ItemStack stack, int uses) {
        stack.getOrCreateTag().putInt(TAG_USES, Math.max(0, uses));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getUsesLeft(stack) < capacity();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getUsesLeft(stack) / capacity());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float fraction = (float) getUsesLeft(stack) / capacity();
        return Mth.hsvToRgb(Math.max(0.0F, fraction) / 3.0F, 1.0F, 1.0F);
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
        if (getUsesLeft(stack) <= 0) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingTicks) {
        int elapsed = USE_TICKS - remainingTicks;
        if (!level.isClientSide) {
            if (elapsed == 4 || elapsed == 12) {
                level.playSound(null, entity.getX(), entity.getEyeY(), entity.getZ(), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.PLAYERS, 0.5F, 1.8F);
            }
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return stack;
        }

        int strength = GumConsumableItem.getStrength(stack);
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
        }
        GumStatusEffects.applyVape(player, GumConsumableItem.getFlavorId(stack), strength);
        GumSlotData.onVapePuff(player, strength);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.CANDLE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 0.9F);
        if (level instanceof ServerLevel serverLevel) {
            Vec3 mouth = player.getEyePosition().add(player.getLookAngle().scale(0.35D));
            serverLevel.sendParticles(ParticleTypes.CLOUD, mouth.x, mouth.y - 0.2D, mouth.z, 10, 0.12D, 0.05D, 0.12D, 0.02D);
        }

        if (player.getAbilities().instabuild) {
            return stack;
        }
        int left = getUsesLeft(stack) - 1;
        if (left <= 0) {
            return new ItemStack(SkofcraftItems.EMPTY_VAPE.get());
        }
        setUsesLeft(stack, left);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.skofcraft.vape_uses", getUsesLeft(stack), capacity()));
        GumConsumableItem.addTraitTooltip(stack, tooltip);
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
                float draw = (t > 0.25F && t < 0.75F) ? raise * (Mth.sin(elapsed * 0.9F) * 0.5F + 0.5F) : 0.0F;
                float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;

                pose.translate(side * Mth.lerp(raise, 0.56F, 0.12F),
                        Mth.lerp(raise, -0.52F, -0.26F) + draw * 0.01F,
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
