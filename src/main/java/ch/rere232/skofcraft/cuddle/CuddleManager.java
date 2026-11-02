package ch.rere232.skofcraft.cuddle;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.config.SkofcraftConfig;
import ch.rere232.skofcraft.network.CuddleSyncS2CPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import ch.rere232.skofcraft.phone.PhoneAddiction;
import ch.rere232.skofcraft.registry.SkofcraftSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cuddles between two players. Nothing happens without both of them agreeing: one asks, the other has to press the
 * key back, and either of them can let go at any time with the key, by sneaking or by walking into a skeleton.
 */
@Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CuddleManager {
    /** Distance between the two players. Their hitboxes are 0.6 wide, so this stops them pushing each other around. */
    public static final double SPACING = 0.62D;

    private static final double MAX_REQUEST_DISTANCE = 6.0D;
    private static final double MAX_DRIFT = 1.5D;
    private static final int REQUEST_TICKS = 15 * 20;
    private static final int REQUEST_COOLDOWN_TICKS = 20 * 20;
    private static final int REMINDER_INTERVAL_TICKS = 40;
    private static final int COMFORT_INTERVAL_TICKS = 40;
    private static final int HEARTS_INTERVAL_TICKS = 30;
    private static final int SOUND_INTERVAL_TICKS = 120;
    private static final int ADDICTION_RELIEF = 3;
    private static final int FIVE_MINUTES_TICKS = 5 * 60 * 20;

    private static final Map<UUID, Request> REQUESTS = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();
    private static final List<Cuddle> CUDDLES = new ArrayList<>();

    private record Request(UUID requester, long expires) {}

    private static final class Cuddle {
        final UUID first;
        final UUID second;
        final int firstId;
        final int secondId;
        int age;

        Cuddle(ServerPlayer first, ServerPlayer second) {
            this.first = first.getUUID();
            this.second = second.getUUID();
            this.firstId = first.getId();
            this.secondId = second.getId();
        }

        boolean involves(UUID player) {
            return first.equals(player) || second.equals(player);
        }
    }

    private CuddleManager() {}

    /** The cuddle key: lets go, accepts a request or asks the player being looked at, in that order. */
    public static void onKey(ServerPlayer player, int targetId) {
        Cuddle current = find(player.getUUID());
        if (current != null) {
            end(player.server, current, "message.skofcraft.cuddle_ended");
            return;
        }
        if (!SkofcraftConfig.enableCuddling) {
            player.displayClientMessage(Component.translatable("message.skofcraft.cuddle_disabled"), true);
            return;
        }
        long now = player.server.overworld().getGameTime();
        Request request = REQUESTS.remove(player.getUUID());
        if (request != null && request.expires > now) {
            ServerPlayer requester = player.server.getPlayerList().getPlayer(request.requester);
            if (requester != null) {
                tryStart(requester, player);
                return;
            }
        }
        ask(player, player.serverLevel().getEntity(targetId) instanceof ServerPlayer target ? target : null, now);
    }

    private static void ask(ServerPlayer player, ServerPlayer target, long now) {
        if (target == null || target == player || !target.isAlive() || target.isSpectator()
                || player.distanceTo(target) > MAX_REQUEST_DISTANCE) {
            player.displayClientMessage(Component.translatable("message.skofcraft.cuddle_no_target"), true);
            return;
        }
        if (COOLDOWNS.getOrDefault(player.getUUID(), 0L) > now) {
            player.displayClientMessage(Component.translatable("message.skofcraft.cuddle_cooldown"), true);
            return;
        }
        Request existing = REQUESTS.get(target.getUUID());
        if (find(target.getUUID()) != null || (existing != null && existing.expires > now)) {
            player.displayClientMessage(Component.translatable("message.skofcraft.cuddle_busy", target.getDisplayName()), true);
            return;
        }
        COOLDOWNS.put(player.getUUID(), now + REQUEST_COOLDOWN_TICKS);
        REQUESTS.put(target.getUUID(), new Request(player.getUUID(), now + REQUEST_TICKS));
        player.displayClientMessage(Component.translatable("message.skofcraft.cuddle_sent", target.getDisplayName()), true);
        remind(target, player);
        target.playNotifySound(SkofcraftSounds.PLUSH_SOFT.get(), SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    private static void remind(ServerPlayer target, ServerPlayer requester) {
        target.displayClientMessage(Component.translatable("message.skofcraft.cuddle_request",
                requester.getDisplayName(), Component.keybind("key.skofcraft.cuddle")), true);
    }

    private static boolean canCuddle(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isPassenger() && !player.isSleeping()
                && !player.isFallFlying() && !player.isShiftKeyDown() && player.onGround() && !player.isInWater()
                && find(player.getUUID()) == null;
    }

    /** The second player has accepted: they step in front of the first one, as long as there is a floor to stand on. */
    private static void tryStart(ServerPlayer first, ServerPlayer second) {
        if (first.level() != second.level() || first.distanceTo(second) > MAX_REQUEST_DISTANCE
                || !canCuddle(first) || !canCuddle(second) || !first.hasLineOfSight(second)) {
            refuse(first, second, "message.skofcraft.cuddle_cant");
            return;
        }
        Vec3 offset = new Vec3(second.getX() - first.getX(), 0.0D, second.getZ() - first.getZ());
        if (offset.lengthSqr() < 1.0E-4D) {
            offset = new Vec3(first.getLookAngle().x, 0.0D, first.getLookAngle().z);
        }
        offset = offset.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : offset.normalize();
        double x = first.getX() + offset.x * SPACING;
        double y = first.getY();
        double z = first.getZ() + offset.z * SPACING;
        AABB box = second.getType().getDimensions().makeBoundingBox(x, y, z);
        if (!second.level().noCollision(second, box) || second.level().noCollision(second, box.move(0.0D, -0.2D, 0.0D))) {
            refuse(first, second, "message.skofcraft.cuddle_no_room");
            return;
        }

        float yaw = (float) (Mth.atan2(-offset.z, -offset.x) * Mth.RAD_TO_DEG) - 90.0F;
        second.connection.teleport(x, y, z, yaw, 0.0F);
        for (ServerPlayer player : List.of(first, second)) {
            player.stopUsingItem();
            player.setSprinting(false);
            player.setDeltaMovement(Vec3.ZERO);
            REQUESTS.remove(player.getUUID());
            SkofcraftAdvancements.award(player, SkofcraftAdvancements.CUDDLE);
        }
        Cuddle cuddle = new Cuddle(first, second);
        CUDDLES.add(cuddle);
        sync(first.server, cuddle, true);
        first.serverLevel().playSound(null, first.getX(), first.getY(), first.getZ(),
                SkofcraftSounds.PLUSH_SOFT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private static void refuse(ServerPlayer first, ServerPlayer second, String message) {
        first.displayClientMessage(Component.translatable(message), true);
        second.displayClientMessage(Component.translatable(message), true);
    }

    private static Cuddle find(UUID player) {
        for (Cuddle cuddle : CUDDLES) {
            if (cuddle.involves(player)) {
                return cuddle;
            }
        }
        return null;
    }

    private static void end(MinecraftServer server, Cuddle cuddle, String message) {
        CUDDLES.remove(cuddle);
        finish(server, cuddle, message);
    }

    private static void finish(MinecraftServer server, Cuddle cuddle, String message) {
        sync(server, cuddle, false);
        for (UUID id : List.of(cuddle.first, cuddle.second)) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                player.displayClientMessage(Component.translatable(message), true);
            }
        }
    }

    private static void sync(MinecraftServer server, Cuddle cuddle, boolean active) {
        CuddleSyncS2CPacket packet = new CuddleSyncS2CPacket(cuddle.firstId, cuddle.secondId, active);
        for (UUID id : List.of(cuddle.first, cuddle.second)) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                SkofcraftNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);
            }
        }
    }

    private static boolean shouldStop(ServerPlayer first, ServerPlayer second) {
        if (first == null || second == null || !first.isAlive() || !second.isAlive() || first.level() != second.level()) {
            return true;
        }
        return first.isPassenger() || second.isPassenger() || first.isSleeping() || second.isSleeping()
                || first.isFallFlying() || second.isFallFlying() || first.isShiftKeyDown() || second.isShiftKeyDown()
                || first.distanceTo(second) > MAX_DRIFT;
    }

    private static void comfort(ServerPlayer player, Cuddle cuddle) {
        if (cuddle.age % COMFORT_INTERVAL_TICKS == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, COMFORT_INTERVAL_TICKS + 20, 0, true, false, true));
            PhoneAddiction.reduce(player, ADDICTION_RELIEF);
        }
        if (cuddle.age == FIVE_MINUTES_TICKS) {
            SkofcraftAdvancements.award(player, SkofcraftAdvancements.FIVE_MORE_MINUTES);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }
        MinecraftServer server = event.getServer();
        tickRequests(server);
        Iterator<Cuddle> iterator = CUDDLES.iterator();
        List<Cuddle> ended = new ArrayList<>();
        while (iterator.hasNext()) {
            Cuddle cuddle = iterator.next();
            ServerPlayer first = server.getPlayerList().getPlayer(cuddle.first);
            ServerPlayer second = server.getPlayerList().getPlayer(cuddle.second);
            if (shouldStop(first, second)) {
                iterator.remove();
                ended.add(cuddle);
                continue;
            }
            cuddle.age++;
            comfort(first, cuddle);
            comfort(second, cuddle);
            ServerLevel level = first.serverLevel();
            double x = (first.getX() + second.getX()) / 2.0D;
            double y = first.getEyeY() + 0.4D;
            double z = (first.getZ() + second.getZ()) / 2.0D;
            if (cuddle.age % HEARTS_INTERVAL_TICKS == 0) {
                level.sendParticles(ParticleTypes.HEART, x, y, z, 1, 0.25D, 0.1D, 0.25D, 0.0D);
            }
            if (cuddle.age % SOUND_INTERVAL_TICKS == 0) {
                level.playSound(null, x, y, z, SkofcraftSounds.PLUSH_SOFT.get(), SoundSource.PLAYERS, 0.4F, 1.0F);
            }
        }
        for (Cuddle cuddle : ended) {
            finish(server, cuddle, "message.skofcraft.cuddle_ended");
        }
    }

    private static void tickRequests(MinecraftServer server) {
        if (REQUESTS.isEmpty()) {
            return;
        }
        long now = server.overworld().getGameTime();
        Iterator<Map.Entry<UUID, Request>> iterator = REQUESTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Request> entry = iterator.next();
            ServerPlayer target = server.getPlayerList().getPlayer(entry.getKey());
            ServerPlayer requester = server.getPlayerList().getPlayer(entry.getValue().requester);
            if (target == null || requester == null) {
                iterator.remove();
            } else if (entry.getValue().expires <= now) {
                iterator.remove();
                requester.displayClientMessage(Component.translatable("message.skofcraft.cuddle_expired", target.getDisplayName()), true);
            } else if ((entry.getValue().expires - now) % REMINDER_INTERVAL_TICKS == 0) {
                remind(target, requester);
            }
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0.0F) {
            Cuddle cuddle = find(player.getUUID());
            if (cuddle != null) {
                end(player.server, cuddle, "message.skofcraft.cuddle_interrupted");
            }
        }
    }

    /** A player who wanders into range of a cuddle in progress needs to hear about it to see it. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer tracked && event.getEntity() instanceof ServerPlayer viewer) {
            Cuddle cuddle = find(tracked.getUUID());
            if (cuddle != null) {
                SkofcraftNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer),
                        new CuddleSyncS2CPacket(cuddle.firstId, cuddle.secondId, true));
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        REQUESTS.clear();
        COOLDOWNS.clear();
        CUDDLES.clear();
    }
}
