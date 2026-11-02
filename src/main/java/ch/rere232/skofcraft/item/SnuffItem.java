package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.config.SkofcraftConfig;
import ch.rere232.skofcraft.gums.GumSlotData;
import ch.rere232.skofcraft.gums.GumStatusEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Consumer;

/** Flavored snuff: hold use to raise the tin to your nose and sniff it for a short burst of effects. */
public class SnuffItem extends Item {
    private static final int USE_TICKS = 36;
    private static final int COOLDOWN_TICKS = 60;

    public SnuffItem(Properties properties) {
        super(properties.stacksTo(16));
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
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingTicks) {
        int elapsed = USE_TICKS - remainingTicks;
        if (level.isClientSide) {
            if (elapsed >= 10 && elapsed <= 26 && elapsed % 2 == 0) {
                spawnInhaleDust(level, entity);
            }
        } else if (elapsed == 8 || elapsed == 20) {
            level.playSound(null, entity.getX(), entity.getEyeY(), entity.getZ(), SkofcraftSounds.SNIFF.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
        }
    }

    private static void spawnInhaleDust(Level level, LivingEntity entity) {
        Vec3 look = entity.getLookAngle();
        Vec3 nose = entity.getEyePosition().add(look.scale(0.3D)).add(0.0D, -0.1D, 0.0D);
        for (int i = 0; i < 2; i++) {
            Vec3 from = nose.add(look.scale(0.25D)).add(
                    (level.random.nextDouble() - 0.5D) * 0.12D,
                    (level.random.nextDouble() - 0.5D) * 0.08D,
                    (level.random.nextDouble() - 0.5D) * 0.12D);
            Vec3 toward = nose.subtract(from).scale(0.12D);
            level.addParticle(new DustParticleOptions(new Vector3f(0.45F, 0.28F, 0.12F), 0.9F),
                    from.x, from.y, from.z, toward.x, toward.y, toward.z);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player) {
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
            }
            GumStatusEffects.applySnuff(player, GumConsumableItem.getFlavorId(stack), SkofcraftConfig.enableNegativeEffects);
            GumSlotData.onSnuffUsed(player);
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

            level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SkofcraftSounds.EXHALE.get(), SoundSource.PLAYERS, 0.4F, 1.0F);
            if (level instanceof ServerLevel serverLevel) {
                Vec3 face = player.getEyePosition().add(player.getLookAngle().scale(0.4D));
                serverLevel.sendParticles(ParticleTypes.POOF, face.x, face.y - 0.1D, face.z, 6, 0.1D, 0.05D, 0.1D, 0.01D);
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        GumConsumableItem.addTraitTooltip(stack, tooltip);
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                if (entity.isUsingItem() && entity.getUsedItemHand() == hand) {
                    return HumanoidModel.ArmPose.SPYGLASS;
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
                float sniff = (t > 0.3F && t < 0.8F) ? raise * (Mth.sin(elapsed * 1.6F) * 0.5F + 0.5F) : 0.0F;
                float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;

                pose.translate(side * Mth.lerp(raise, 0.56F, 0.14F),
                        Mth.lerp(raise, -0.52F, -0.12F) + sniff * 0.015F,
                        Mth.lerp(raise, -0.72F, -0.5F) - sniff * 0.03F);
                pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(raise, 0.0F, -25.0F)));
                pose.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(raise, 0.0F, -30.0F)));
                pose.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(raise, 0.0F, 15.0F)));
                return true;
            }
        });
    }

    private static float ease(float x) {
        return x * x * (3.0F - 2.0F * x);
    }
}
