package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.registry.SkofcraftEntities;
import ch.rere232.skofcraft.registry.SkofcraftSounds;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.List;

/**
 * An adult tank mechanic drawn in an anime style. Spot a tank with room on it and they'll walk over and climb on,
 * and stay until they're asked to get off. Turn up near tanks, and now and then in villages.
 */
public class AnimeCrew extends PathfinderMob {
    public static final int VARIANTS = 4;
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(AnimeCrew.class, EntityDataSerializers.INT);
    private static final String[] NAMES = {"Akari", "Rin", "Yuna", "Sora", "Nao", "Emi", "Kaede", "Tomoko"};
    private static final double NOTICE_RANGE = 24.0D;
    private static final double BOARD_RANGE = 5.0D;
    private static final double WALK_SPEED = 1.2D;
    private static final int SEARCH_INTERVAL = 20;
    /** How long after being asked off before they'll climb back on. */
    private static final int STAY_OFF_TICKS = 20 * 60;

    private int stayOffTicks;

    public AnimeCrew(EntityType<? extends AnimeCrew> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0D).add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
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
    public void tick() {
        super.tick();
        if (getVehicle() instanceof Tank tank) {
            // Facing the way the tank is going
            setYRot(tank.getYRot());
            setYBodyRot(tank.getYRot());
            return;
        }
        if (!(level() instanceof ServerLevel)) {
            return;
        }
        if (stayOffTicks > 0) {
            stayOffTicks--;
        } else if (tickCount % SEARCH_INTERVAL == 0) {
            headForTank();
        }
    }

    private void headForTank() {
        List<Tank> tanks = level().getEntitiesOfClass(Tank.class, getBoundingBox().inflate(NOTICE_RANGE), Tank::hasCrewSeat);
        tanks.stream().min(Comparator.comparingDouble(this::distanceToSqr)).ifPresent(tank -> {
            if (distanceTo(tank) <= BOARD_RANGE) {
                board(tank);
            } else {
                getNavigation().moveTo(tank, WALK_SPEED);
            }
        });
    }

    private void board(Tank tank) {
        if (!startRiding(tank)) {
            return;
        }
        getNavigation().stop();
        playSound(SkofcraftSounds.FEMBOY_HUM.get(), 1.0F, 1.2F);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HEART, getX(), getEyeY() + 0.5D, getZ(), 3, 0.3D, 0.2D, 0.3D, 0.0D);
        }
        if (tank.getControllingPassenger() instanceof ServerPlayer driver) {
            SkofcraftAdvancements.award(driver, SkofcraftAdvancements.TANK_CREW);
        }
    }

    /** Right-click one riding a tank to ask them to get off. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isPassenger() && player.getItemInHand(hand).isEmpty()) {
            if (!level().isClientSide) {
                stopRiding();
                stayOffTicks = STAY_OFF_TICKS;
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SkofcraftSounds.FEMBOY_HUM.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", getVariant());
        tag.putInt("StayOff", stayOffTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(VARIANT, Math.floorMod(tag.getInt("Variant"), VARIANTS));
        stayOffTicks = tag.getInt("StayOff");
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** Named apart from the other mobs' nested classes: Forge names its generated handlers after the simple class name. */
    @Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class AnimeCrewAttributes {
        private AnimeCrewAttributes() {}

        @SubscribeEvent
        public static void onAttributes(EntityAttributeCreationEvent event) {
            event.put(SkofcraftEntities.ANIME_CREW.get(), createAttributes().build());
        }
    }

    /** Once a minute, each player near a tank with room on it has a fair chance of company; in a village, a small one. */
    @Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class AnimeCrewSpawner {
        private static final int INTERVAL_TICKS = 20 * 60;
        private static final float TANK_CHANCE = 0.10F;
        private static final float VILLAGE_CHANCE = 0.015F;
        private static final double TANK_RANGE = 64.0D;
        private static final double CROWD_RADIUS = 96.0D;
        private static final int MAX_NEARBY = 6;
        private static final int RANGE = 32;
        private static final int MIN_DISTANCE = 16;

        private AnimeCrewSpawner() {}

        @SubscribeEvent
        public static void onLevelTick(TickEvent.LevelTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                    || level.getGameTime() % INTERVAL_TICKS != 0 || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
                return;
            }
            for (ServerPlayer player : level.players()) {
                if (player.isSpectator()) {
                    continue;
                }
                boolean nearTank = !level.getEntitiesOfClass(Tank.class, player.getBoundingBox().inflate(TANK_RANGE), Tank::hasCrewSeat).isEmpty();
                float chance = nearTank ? TANK_CHANCE : level.isVillage(player.blockPosition()) ? VILLAGE_CHANCE : 0.0F;
                if (level.random.nextFloat() < chance) {
                    trySpawnNear(level, player);
                }
            }
        }

        private static void trySpawnNear(ServerLevel level, ServerPlayer player) {
            if (level.getEntitiesOfClass(AnimeCrew.class, player.getBoundingBox().inflate(CROWD_RADIUS)).size() >= MAX_NEARBY) {
                return;
            }
            EntityType<AnimeCrew> type = SkofcraftEntities.ANIME_CREW.get();
            for (int attempt = 0; attempt < 10; attempt++) {
                int x = player.getBlockX() + level.random.nextInt(RANGE * 2 + 1) - RANGE;
                int z = player.getBlockZ() + level.random.nextInt(RANGE * 2 + 1) - RANGE;
                BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
                if (pos.distSqr(player.blockPosition()) < MIN_DISTANCE * MIN_DISTANCE
                        || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                        || !level.getFluidState(pos).isEmpty()
                        || !level.noCollision(type.getAABB(x + 0.5D, pos.getY(), z + 0.5D))) {
                    continue;
                }
                AnimeCrew crew = type.create(level);
                if (crew == null) {
                    return;
                }
                crew.moveTo(x + 0.5D, pos.getY(), z + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
                crew.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
                level.addFreshEntity(crew);
                return;
            }
        }
    }
}
