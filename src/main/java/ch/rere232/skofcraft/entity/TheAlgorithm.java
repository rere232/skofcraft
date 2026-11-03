package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.phone.PhoneAddiction;
import ch.rere232.skofcraft.registry.SkofcraftSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * The thing behind the feed. Fed a phone at a Heart Altar, it walks out of the screen. It calls up Gloom, and now and
 * then every player nearby gets a notification they can't look away from. Drops the Do Not Disturb Charm.
 */
public class TheAlgorithm extends Gloom {
    private static final int SUMMON_INTERVAL = 200;
    private static final int NOTIFY_INTERVAL = 140;
    private static final int MAX_GLOOM = 6;
    private static final double RANGE = 32.0D;
    private static final int NOTIFY_ADDICTION = 3;

    private final ServerBossEvent bossBar = new ServerBossEvent(Component.translatable("entity.skofcraft.the_algorithm"),
            BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.NOTCHED_10);

    public TheAlgorithm(EntityType<? extends TheAlgorithm> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAlgorithmAttributes() {
        return createAttributes().add(Attributes.MAX_HEALTH, 300.0D).add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D).add(Attributes.FOLLOW_RANGE, 48.0D).add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    public void tick() {
        // Instead of falling off the island with everyone's loot, it comes back to whoever it was chasing
        if (!level().isClientSide && getY() < level().getMinBuildHeight() + 2.0D) {
            LivingEntity target = getTarget();
            BlockPos landing = target != null ? target.blockPosition()
                    : level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, BlockPos.ZERO);
            teleportTo(landing.getX() + 0.5D, landing.getY() + 1.0D, landing.getZ() + 0.5D);
            setDeltaMovement(0.0D, 0.0D, 0.0D);
        }
        super.tick();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossBar.setProgress(getHealth() / getMaxHealth());
        if (!(level() instanceof ServerLevel level) || getTarget() == null) {
            return;
        }
        if (tickCount % SUMMON_INTERVAL == 0 && level.getEntitiesOfClass(Gloom.class, area()).size() <= MAX_GLOOM) {
            for (int i = 0; i < 2; i++) {
                Mob gloom = Gloom.create(level);
                if (gloom != null) {
                    gloom.moveTo(getX() + random.nextInt(7) - 3, getY(), getZ() + random.nextInt(7) - 3, 0.0F, 0.0F);
                    level.addFreshEntity(gloom);
                }
            }
        }
        if (tickCount % NOTIFY_INTERVAL == 0) {
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area())) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, true, false, true));
                PhoneAddiction.add(player, NOTIFY_ADDICTION);
                level.playSound(null, player.blockPosition(), SkofcraftSounds.PHONE_NOTIFY.get(), SoundSource.HOSTILE, 1.5F, 1.0F);
            }
        }
    }

    private AABB area() {
        return getBoundingBox().inflate(RANGE);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (source.getEntity() instanceof Player killer) {
            SkofcraftAdvancements.award(killer, SkofcraftAdvancements.UNSUBSCRIBED);
        }
    }
}
