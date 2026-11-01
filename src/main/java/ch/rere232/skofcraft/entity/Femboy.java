package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.registry.SkofcraftEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.GolemRandomStrollInVillageGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** A village femboy: wanders around the village and can't be hurt. Hitting one pats their head instead. */
public class Femboy extends PathfinderMob {
    public static final int VARIANTS = 4;
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(Femboy.class, EntityDataSerializers.INT);
    private static final String[] NAMES = {"Felix", "Ezra", "Milo", "Theo", "Remy", "Jules", "Kai", "Sasha", "Robin", "Charlie", "Nico", "Eli"};
    private static final int PAT_COOLDOWN_TICKS = 10;

    private int patCooldown;

    public Femboy(EntityType<? extends Femboy> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(2, new GolemRandomStrollInVillageGoal(this, 0.6D));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(VARIANT, 0);
    }

    public int getVariant() {
        return entityData.get(VARIANT);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        SpawnGroupData data, CompoundTag tag) {
        RandomSource random = level.getRandom();
        entityData.set(VARIANT, random.nextInt(VARIANTS));
        if (!hasCustomName()) {
            setCustomName(Component.literal(NAMES[random.nextInt(NAMES.length)]));
        }
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", getVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(VARIANT, Math.floorMod(tag.getInt("Variant"), VARIANTS));
    }

    @Override
    public void tick() {
        super.tick();
        if (patCooldown > 0) {
            patCooldown--;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Only /kill and the void get through
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        if (!level().isClientSide && source.getDirectEntity() instanceof Player player) {
            pat(player);
        }
        return false;
    }

    private void pat(Player player) {
        if (patCooldown > 0) {
            return;
        }
        patCooldown = PAT_COOLDOWN_TICKS;
        getNavigation().stop();
        getLookControl().setLookAt(player);
        playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.4F + random.nextFloat() * 0.3F);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART, getX(), getEyeY() + 0.6D, getZ(), 3, 0.3D, 0.1D, 0.3D, 0.0D);
        }
        player.displayClientMessage(Component.translatable("message.skofcraft.headpat", getDisplayName()), true);
        SkofcraftAdvancements.award(player, SkofcraftAdvancements.HEADPATS);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Registration {
        @SubscribeEvent
        public static void onAttributes(EntityAttributeCreationEvent event) {
            event.put(SkofcraftEntities.FEMBOY.get(), createAttributes().build());
        }
    }

    /**
     * Natural spawning: once a minute, each player standing in a village has a small chance of a femboy showing up
     * somewhere nearby in the village, out of sight range, as long as there isn't already one around.
     */
    @Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Spawner {
        private static final int INTERVAL_TICKS = 20 * 60;
        private static final float CHANCE = 0.015F;
        private static final int RANGE = 40;
        private static final double CROWD_RADIUS = 96.0D;

        @SubscribeEvent
        public static void onLevelTick(TickEvent.LevelTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                    || level.getGameTime() % INTERVAL_TICKS != 0 || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
                return;
            }
            for (ServerPlayer player : level.players()) {
                if (player.isSpectator() || level.random.nextFloat() >= CHANCE || !level.isVillage(player.blockPosition())) {
                    continue;
                }
                trySpawnNear(level, player);
            }
        }

        private static void trySpawnNear(ServerLevel level, ServerPlayer player) {
            if (!level.getEntitiesOfClass(Femboy.class, player.getBoundingBox().inflate(CROWD_RADIUS)).isEmpty()) {
                return;
            }
            EntityType<Femboy> type = SkofcraftEntities.FEMBOY.get();
            for (int attempt = 0; attempt < 10; attempt++) {
                int x = player.getBlockX() + level.random.nextInt(RANGE * 2 + 1) - RANGE;
                int z = player.getBlockZ() + level.random.nextInt(RANGE * 2 + 1) - RANGE;
                BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
                if (pos.distSqr(player.blockPosition()) < 16 * 16 || !level.isVillage(pos)
                        || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                        || !level.getFluidState(pos).isEmpty()
                        || !level.noCollision(type.getAABB(x + 0.5D, pos.getY(), z + 0.5D))) {
                    continue;
                }
                Femboy femboy = type.create(level);
                if (femboy == null) {
                    return;
                }
                femboy.moveTo(x + 0.5D, pos.getY(), z + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
                femboy.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
                level.addFreshEntity(femboy);
                return;
            }
        }
    }
}
