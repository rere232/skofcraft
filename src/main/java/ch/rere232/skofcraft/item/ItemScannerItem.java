package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.registry.SkofcraftSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Locale;

/**
 * A supermarket barcode scanner that works as a one-shot taser: right-click to fire a beam a few blocks long. Anything
 * it hits dies, through armor and shields, unless it's a boss. Hit or miss, it then has to recharge.
 */
public class ItemScannerItem extends Item {
    private static final ResourceKey<DamageType> SCANNED =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "scanned"));
    private static final double RANGE = 5.5D;
    private static final int RECHARGE_TICKS = 20 * 30;
    private static final float LETHAL_DAMAGE = 1000.0F;
    private static final float BOSS_DAMAGE = 20.0F;
    private static final double BEAM_STEP = 0.25D;
    private static final double BEAM_START = 0.5D;
    private static final DustParticleOptions BEAM = new DustParticleOptions(new Vector3f(1.0F, 0.05F, 0.05F), 0.5F);
    private static final float PRICE_PER_HEALTH = 0.95F;

    public ItemScannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            player.getCooldowns().addCooldown(this, RECHARGE_TICKS);
            fire(serverLevel, player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void fire(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(RANGE));
        // Walls stop the beam
        HitResult wall = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (wall.getType() != HitResult.Type.MISS) {
            end = wall.getLocation();
        }
        AABB search = player.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, search,
                entity -> entity instanceof LivingEntity && entity.isPickable() && !entity.isSpectator() && entity != player.getVehicle(),
                eye.distanceToSqr(end));

        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SkofcraftSounds.ITEM_SCANNER_BEEP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        drawBeam(level, eye, hit != null ? hit.getLocation() : end);
        if (hit == null) {
            player.displayClientMessage(Component.translatable("message.skofcraft.item_scanner.miss").withStyle(ChatFormatting.RED), true);
            return;
        }
        scan(level, player, (LivingEntity) hit.getEntity());
    }

    private static void drawBeam(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 direction = to.subtract(from);
        double length = direction.length();
        for (double d = BEAM_START; d < length; d += BEAM_STEP) {
            Vec3 at = from.add(direction.scale(d / length));
            level.sendParticles(BEAM, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void scan(ServerLevel level, Player player, LivingEntity target) {
        Component name = target.getDisplayName();
        String price = String.format(Locale.ROOT, "%.2f", target.getMaxHealth() * PRICE_PER_HEALTH);
        DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SCANNED), player);
        boolean boss = target.getType().is(Tags.EntityTypes.BOSSES);
        // Ignore the moment of immunity after a recent hit, or the shot could land and do nothing
        target.invulnerableTime = 0;
        boolean hurt = target.hurt(source, boss ? BOSS_DAMAGE : LETHAL_DAMAGE);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.5D), target.getZ(), 30, 0.3D, 0.5D, 0.3D, 0.2D);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SkofcraftSounds.TASER_ZAP.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
        if (hurt) {
            player.displayClientMessage(Component.translatable("message.skofcraft.item_scanner.price", name, price).withStyle(ChatFormatting.GREEN), true);
        }
        if (!target.isAlive()) {
            SkofcraftAdvancements.award(player, SkofcraftAdvancements.UNEXPECTED_ITEM);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.skofcraft.item_scanner").withStyle(ChatFormatting.GRAY));
    }
}
