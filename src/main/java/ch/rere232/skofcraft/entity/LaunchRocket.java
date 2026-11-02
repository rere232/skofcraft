package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.registry.SkofcraftEntities;
import ch.rere232.skofcraft.space.OrbitData;
import ch.rere232.skofcraft.space.SatelliteType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

/**
 * The launcher rocket carrying a satellite. It sits on the pad through a {@link #COUNTDOWN}-tick countdown, lights
 * its engine, climbs faster and faster through the sky and, well above the build limit, drops off its payload
 * into orbit and is gone.
 */
public class LaunchRocket extends Entity {
    public static final int COUNTDOWN = 100;
    private static final double ACCELERATION = 0.025D;
    private static final double MAX_SPEED = 2.2D;
    private static final double ORBIT_ABOVE_BUILD_LIMIT = 160.0D;
    private static final double ANNOUNCE_RANGE = 96.0D;
    private static final double BLAST_RADIUS = 3.5D;
    private static final double VIEW_RANGE = 512.0D;
    private static final EntityDataAccessor<Integer> TIMER = SynchedEntityData.defineId(LaunchRocket.class, EntityDataSerializers.INT);

    private SatelliteType payload = SatelliteType.RECON;
    protected UUID owner = new UUID(0L, 0L);
    private double speed;
    /** Client: where the pad is, for the exhaust billowing across the ground; NaN until known. */
    private double padY = Double.NaN;

    public LaunchRocket(EntityType<? extends LaunchRocket> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static LaunchRocket create(ServerLevel level, BlockPos pad, double padTop, SatelliteType payload, UUID owner) {
        LaunchRocket rocket = new LaunchRocket(SkofcraftEntities.LAUNCH_ROCKET.get(), level);
        rocket.setPos(pad.getX() + 0.5D, pad.getY() + padTop, pad.getZ() + 0.5D);
        rocket.payload = payload;
        rocket.owner = owner;
        return rocket;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(TIMER, 0);
    }

    public int timer() {
        return entityData.get(TIMER);
    }

    /** Ticks on the pad before the engine lights. */
    public int countdown() {
        return COUNTDOWN;
    }

    /** Called every countdown tick on the server, for extra warnings. */
    protected void onCountdown(ServerLevel level, int timer) {
    }

    @Override
    public void tick() {
        super.tick();
        int timer = timer();
        if (level().isClientSide) {
            clientEffects(timer);
            return;
        }
        ServerLevel level = (ServerLevel) level();
        timer++;
        entityData.set(TIMER, timer);
        if (timer < countdown()) {
            onCountdown(level, timer);
            if (timer % 20 == 0) {
                announce(level, Component.translatable("message.skofcraft.launch_countdown", (countdown() - timer) / 20).withStyle(ChatFormatting.GOLD));
                level.playSound(null, getX(), getY(), getZ(), SkofcraftSounds.COUNTDOWN_BEEP.get(), SoundSource.BLOCKS, 3.0F, 1.0F);
            }
            return;
        }
        if (timer == countdown()) {
            liftoff(level);
        }
        speed = Math.min(MAX_SPEED, speed + ACCELERATION);
        setDeltaMovement(0.0D, speed, 0.0D);
        setPos(getX(), getY() + speed, getZ());
        if (timer % 4 == 0) {
            level.playSound(null, getX(), getY(), getZ(), SkofcraftSounds.ENGINE_IGNITE.get(), SoundSource.BLOCKS, 6.0F, 1.0F);
        }
        if (getY() > level.getMaxBuildHeight() + ORBIT_ABOVE_BUILD_LIMIT) {
            reachOrbit(level);
        }
    }

    protected void announce(ServerLevel level, Component text) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(this) < ANNOUNCE_RANGE * ANNOUNCE_RANGE) {
                player.displayClientMessage(text, true);
            }
        }
    }

    protected void liftoff(ServerLevel level) {
        announce(level, Component.translatable("message.skofcraft.launch_liftoff").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        level.playSound(null, getX(), getY(), getZ(), SkofcraftSounds.EXPLOSION.get(), SoundSource.BLOCKS, 8.0F, 1.0F);
        level.playSound(null, getX(), getY(), getZ(), SkofcraftSounds.MISSILE_ROAR.get(), SoundSource.BLOCKS, 8.0F, 1.0F);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(this) < VIEW_RANGE * VIEW_RANGE) {
                level.sendParticles(player, ParticleTypes.EXPLOSION_EMITTER, true, getX(), getY(), getZ(), 2, 1.0D, 0.2D, 1.0D, 0.0D);
            }
        }
        // Standing next to the pad at ignition is a bad idea
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(BLAST_RADIUS, 1.0D, BLAST_RADIUS))) {
            victim.setSecondsOnFire(5);
            victim.hurt(damageSources().inFire(), 6.0F);
        }
    }

    protected void reachOrbit(ServerLevel level) {
        OrbitData orbit = OrbitData.get(level);
        orbit.add(payload, owner, level.getGameTime());
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.skofcraft.satellite_in_orbit",
                    Component.translatable(payload.key()), orbit.count(SatelliteType.RECON), orbit.count(SatelliteType.STRIKE))
                    .withStyle(ChatFormatting.AQUA), false);
            SkofcraftAdvancements.award(player, SkofcraftAdvancements.SATELLITE_LAUNCH);
        }
        discard();
    }

    private void clientEffects(int timer) {
        if (timer <= countdown() && Double.isNaN(padY)) {
            padY = getY();
        }
        if (timer >= countdown() - 30 && timer < countdown()) {
            // Venting before ignition
            for (int i = 0; i < 2; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                level().addParticle(ParticleTypes.CLOUD, getX() + Math.cos(angle) * 0.5D, getY() + 1.0D + random.nextDouble() * 2.0D,
                        getZ() + Math.sin(angle) * 0.5D, Math.cos(angle) * 0.15D, 0.0D, Math.sin(angle) * 0.15D);
            }
            return;
        }
        if (timer < countdown()) {
            return;
        }
        for (int i = 0; i < 8; i++) {
            double ox = (random.nextDouble() - 0.5D) * 0.4D;
            double oz = (random.nextDouble() - 0.5D) * 0.4D;
            level().addAlwaysVisibleParticle(i % 4 == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.FLAME, true,
                    getX() + ox, getY() - 0.3D - random.nextDouble() * 1.5D, getZ() + oz, ox * 0.3D, -0.6D - random.nextDouble() * 0.4D, oz * 0.3D);
        }
        level().addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, getX(), getY() - 2.0D, getZ(),
                (random.nextDouble() - 0.5D) * 0.05D, 0.02D, (random.nextDouble() - 0.5D) * 0.05D);
        // Exhaust spreading across the ground while the rocket is still low
        if (!Double.isNaN(padY) && getY() - padY < 30.0D) {
            for (int i = 0; i < 10; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                level().addParticle(ParticleTypes.CLOUD, getX(), padY + 0.3D, getZ(),
                        Math.cos(angle) * 0.6D, 0.02D, Math.sin(angle) * 0.6D);
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < VIEW_RANGE * VIEW_RANGE;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(TIMER, tag.getInt("Timer"));
        payload = SatelliteType.byId(tag.getInt("Payload"));
        if (tag.hasUUID("Owner")) {
            owner = tag.getUUID("Owner");
        }
        speed = tag.getDouble("Speed");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Timer", timer());
        tag.putInt("Payload", payload.ordinal());
        tag.putUUID("Owner", owner);
        tag.putDouble("Speed", speed);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
