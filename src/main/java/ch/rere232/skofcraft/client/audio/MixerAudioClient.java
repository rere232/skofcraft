package ch.rere232.skofcraft.client.audio;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.audio.MixerTrack;
import ch.rere232.skofcraft.blockentity.DjMixerBlockEntity;
import ch.rere232.skofcraft.network.MixerReadyC2SPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Decides which mixers this client should be hearing. A mixer is streamed while it plays, has speakers and the
 * player is near one; seeking, skipping or resuming bumps the mixer's revision and the stream is restarted.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class MixerAudioClient {
    /** Full volume up to this far from a speaker... */
    private static final double FULL_RANGE = 6.0D;
    /** ...fading out to nothing here. */
    public static final double RANGE = 48.0D;
    /** A stream already going is kept a bit further out, so walking along the edge does not restart it over and over. */
    private static final double KEEP_RANGE = RANGE + 16.0D;
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_TICKS = 200;
    private static final int PARTICLE_INTERVAL = 8;

    private static final Set<BlockPos> MIXERS = new HashSet<>();
    private static final Map<BlockPos, MixerSession> SESSIONS = new HashMap<>();
    private static final Map<BlockPos, Retry> RETRIES = new HashMap<>();
    private static int ticks;
    /** The last tools message shown in chat, so each one is only said once. */
    @Nullable private static String hinted;

    private record Retry(int revision, int count, int notBefore) {}

    private MixerAudioClient() {}

    public static void track(BlockPos pos) {
        MIXERS.add(pos.immutable());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            stopAll();
            return;
        }
        ticks++;
        Vec3 listener = mc.gameRenderer.getMainCamera().getPosition();
        Iterator<BlockPos> iterator = MIXERS.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (!(mc.level.getBlockEntity(pos) instanceof DjMixerBlockEntity mixer)) {
                iterator.remove();
                stop(pos);
                RETRIES.remove(pos);
                continue;
            }
            update(pos, mixer, listener);
        }
    }

    private static void update(BlockPos pos, DjMixerBlockEntity mixer, Vec3 listener) {
        MixerSession session = SESSIONS.get(pos);
        MixerTrack track = mixer.currentTrack();
        double distance = nearestSpeaker(mixer, listener);
        boolean audible = mixer.isPlaying() && track != null && distance <= (session != null ? KEEP_RANGE : RANGE);
        if (audible && !MediaTools.ready()) {
            // Players who never opened a mixer still need the tools looked for, and told once if they are missing
            MediaTools.ensureChecked();
            MediaTools.State state = MediaTools.state();
            String key = state == MediaTools.State.INSTALLING ? "downloading"
                    : state == MediaTools.State.MISSING || state == MediaTools.State.FAILED ? "needs_tools" : null;
            if (key != null && !key.equals(hinted) && Minecraft.getInstance().player != null) {
                hinted = key;
                Minecraft.getInstance().player.displayClientMessage(Component.translatable("message.skofcraft.dj_mixer." + key), false);
            }
        }
        boolean wanted = audible && MediaTools.ready();
        if (!wanted) {
            stop(pos);
            return;
        }
        boolean outdated = session == null || session.revision != mixer.revision() || !session.track.id().equals(track.id());
        if (!outdated && session.droppedOut()) {
            Retry retry = RETRIES.get(pos);
            int count = retry != null && retry.revision() == mixer.revision() ? retry.count() : 0;
            if (count >= MAX_RETRIES || (retry != null && ticks < retry.notBefore())) {
                session.tick(mixer::elapsedMs, 0.0F, false, () -> { });
                return;
            }
            RETRIES.put(pos, new Retry(mixer.revision(), count + 1, ticks + RETRY_DELAY_TICKS));
            outdated = true;
        }
        if (outdated) {
            stop(pos);
            session = new MixerSession(mixer.revision(), track, mixer.elapsedMs());
            SESSIONS.put(pos, session);
            session.start();
        }
        int revision = mixer.revision();
        session.tick(mixer::elapsedMs, gain(mixer, distance), mixer.isWaiting(),
                () -> SkofcraftNetwork.CHANNEL.sendToServer(new MixerReadyC2SPacket(pos, revision)));
        // Fetch the next song while this one plays, so it starts the moment its turn comes
        int next = mixer.upNext();
        if (next >= 0 && next < mixer.queue().size() && session.phase() == MixerSession.Phase.PLAYING) {
            TrackCache.prefetch(mixer.queue().get(next));
        }
        if (session.phase() == MixerSession.Phase.PLAYING && ticks % PARTICLE_INTERVAL == 0) {
            spawnNotes(mixer, listener);
        }
    }

    private static double nearestSpeaker(DjMixerBlockEntity mixer, Vec3 listener) {
        double best = Double.MAX_VALUE;
        for (BlockPos speaker : mixer.speakers()) {
            best = Math.min(best, listener.distanceTo(Vec3.atCenterOf(speaker)));
        }
        return best;
    }

    private static float gain(DjMixerBlockEntity mixer, double distance) {
        double falloff = distance <= FULL_RANGE ? 1.0D : Math.max(0.0D, 1.0D - (distance - FULL_RANGE) / (RANGE - FULL_RANGE));
        return (float) (falloff * falloff * mixer.volume() / 100.0D);
    }

    private static void spawnNotes(DjMixerBlockEntity mixer, Vec3 listener) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        for (BlockPos speaker : mixer.speakers()) {
            if (listener.distanceToSqr(Vec3.atCenterOf(speaker)) < 32.0D * 32.0D && mc.level.random.nextInt(3) == 0) {
                mc.level.addParticle(ParticleTypes.NOTE, speaker.getX() + 0.5D, speaker.getY() + 1.2D, speaker.getZ() + 0.5D,
                        mc.level.random.nextInt(25) / 24.0D, 0.0D, 0.0D);
            }
        }
    }

    private static void stop(BlockPos pos) {
        MixerSession session = SESSIONS.remove(pos);
        if (session != null) {
            session.stop();
        }
    }

    private static void stopAll() {
        SESSIONS.values().forEach(MixerSession::stop);
        SESSIONS.clear();
        MIXERS.clear();
        RETRIES.clear();
    }

    /** Every frame: shake the camera for bass hits the speakers are playing right now. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && !mc.isPaused()) {
            for (Map.Entry<BlockPos, MixerSession> entry : SESSIONS.entrySet()) {
                if (mc.level.getBlockEntity(entry.getKey()) instanceof DjMixerBlockEntity mixer && mixer.isPlaying()) {
                    // The mixer clock moves in 50 ms ticks; the partial tick puts hits on the right frame
                    long now = mixer.elapsedMs() + (long) (event.getPartialTick() * 50.0D);
                    float hit = entry.getValue().bassHit(now);
                    if (hit > 0.0F) {
                        BassShake.kick(hit);
                    }
                }
            }
        }
        BassShake.apply(event);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        stopAll();
    }

    /** What this client's stream of the mixer at {@code pos} is doing, for the screen; null when nothing is streaming. */
    @Nullable
    public static String status(BlockPos pos) {
        MixerSession session = SESSIONS.get(pos);
        if (session == null) {
            return null;
        }
        return switch (session.phase()) {
            case RESOLVING -> session.cached() ? "Loading..." : "Getting the song from YouTube...";
            case BUFFERING -> "Starting...";
            case PLAYING -> "";
            case ENDED -> session.droppedOut() ? "Stream dropped" : "";
            case FAILED -> "Error: " + session.error();
        };
    }
}
