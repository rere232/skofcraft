package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.config.SkofcraftConfig;
import ch.rere232.skofcraft.gums.GumSlotData;
import ch.rere232.skofcraft.gums.GumStatusEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** A cigarette, cigarillo or cigar: hold use to light and smoke it, which consumes it. */
public class SmokeItem extends Item {
    /** How long it takes to smoke, the cooldown after, and what it does. */
    public record Profile(int useTicks, int cooldownTicks, int effectTicks, int amplifier,
                          boolean resistance, int weaknessTicks, int addiction) {}

    public static final Profile CIGARETTE = new Profile(48, 100, 1200, 0, false, 200, 2);
    public static final Profile CIGARILLO = new Profile(72, 160, 2400, 0, false, 300, 3);
    public static final Profile CIGAR = new Profile(120, 300, 3600, 1, true, 400, 4);

    private final Profile profile;

    public SmokeItem(Properties properties, Profile profile) {
        super(properties);
        this.profile = profile;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return profile.useTicks();
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
        int total = profile.useTicks();
        int elapsed = total - remainingTicks;
        if (level.isClientSide) {
            if (elapsed >= 10 && elapsed % 6 == 0) {
                Vec3 mouth = entity.getEyePosition().add(entity.getLookAngle().scale(0.4D));
                level.addParticle(ParticleTypes.SMOKE, mouth.x, mouth.y - 0.25D, mouth.z, 0.0D, 0.02D, 0.0D);
            }
        } else if (elapsed == 2) {
            level.playSound(null, entity.getX(), entity.getEyeY(), entity.getZ(), SkofcraftSounds.LIGHTER.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
        } else if (elapsed == total * 3 / 8 || elapsed == total * 3 / 4) {
            level.playSound(null, entity.getX(), entity.getEyeY(), entity.getZ(), SkofcraftSounds.INHALE.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player) {
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
            }
            GumStatusEffects.applySmoke(player, profile.effectTicks(), profile.amplifier(), profile.resistance(),
                    profile.weaknessTicks(), SkofcraftConfig.enableNegativeEffects);
            GumSlotData.onSmoked(player, profile.addiction());
            player.getCooldowns().addCooldown(this, profile.cooldownTicks());

            level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SkofcraftSounds.EXHALE.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
            if (level instanceof ServerLevel serverLevel) {
                Vec3 mouth = player.getEyePosition().add(player.getLookAngle().scale(0.4D));
                int puffs = 10 + profile.addiction() * 3;
                serverLevel.sendParticles(ParticleTypes.CLOUD, mouth.x, mouth.y - 0.25D, mouth.z, puffs, 0.15D, 0.06D, 0.15D, 0.02D);
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return stack;
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
                // Item's constructor calls initializeClient before profile is assigned, so read it here.
                int total = profile.useTicks();
                float elapsed = total - (player.getUseItemRemainingTicks() - partialTick);
                float t = Mth.clamp(elapsed / total, 0.0F, 1.0F);
                float raise = t < 0.15F ? ease(t / 0.15F) : (t > 0.88F ? ease((1.0F - t) / 0.12F) : 1.0F);
                // Slow drags: the hand dips away from the mouth in between.
                float drag = (t > 0.3F && t < 0.85F) ? 0.5F + 0.5F * Mth.cos(elapsed * 0.35F) : 1.0F;
                float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
                float near = raise * drag;

                pose.translate(side * Mth.lerp(near, 0.56F, 0.1F),
                        Mth.lerp(near, -0.52F, -0.24F),
                        Mth.lerp(near, -0.72F, -0.46F));
                pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(near, 0.0F, -15.0F)));
                pose.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(near, 0.0F, -40.0F)));
                pose.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(near, 0.0F, 30.0F)));
                return true;
            }
        });
    }

    private static float ease(float x) {
        return x * x * (3.0F - 2.0F * x);
    }
}
