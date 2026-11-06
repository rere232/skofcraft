package ch.rere232.skofcraft.client.audio;

import ch.rere232.skofcraft.audio.MixerTrack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.function.LongSupplier;

/**
 * One song playing from one mixer on this client: yt-dlp downloads the song's WebM/Opus audio, the mod decodes it
 * in Java from the mixer's position, and once a few seconds are buffered the sound starts, trimmed to match the
 * mixer's clock exactly.
 */
@OnlyIn(Dist.CLIENT)
final class MixerSession {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PREBUFFER = PcmStream.BYTES_PER_SECOND * 3;

    enum Phase { RESOLVING, BUFFERING, PLAYING, FAILED, ENDED }

    final int revision;
    final MixerTrack track;
    private final long offsetMs;
    private final PcmStream stream = new PcmStream();
    private volatile Phase phase = Phase.RESOLVING;
    private volatile String error = "";
    @Nullable private volatile Process download;
    @Nullable private MixerSound sound;
    private volatile boolean stopped;
    /** The decoder got to the end of the song, as opposed to the stream being cut off. */
    private volatile boolean decoderDone;

    MixerSession(int revision, MixerTrack track, long offsetMs) {
        this.revision = revision;
        this.track = track;
        this.offsetMs = offsetMs;
    }

    void start() {
        MediaTools.WORKER.submit(this::run);
    }

    /** Downloads and decodes until the song ends or the session stops. Runs on a worker. */
    private void run() {
        Process process;
        try {
            process = MediaTools.startAudioDownload(track);
        } catch (Exception e) {
            fail(e.getMessage() == null ? e.toString() : e.getMessage());
            return;
        }
        download = process;
        if (stopped) {
            process.destroyForcibly();
            return;
        }
        boolean[] gotAudio = {false};
        boolean webm = false;
        try (InputStream in = process.getInputStream()) {
            webm = WebmOpusDecoder.decode(in, offsetMs, (data, length) -> {
                if (stopped) {
                    return false;
                }
                if (!gotAudio[0]) {
                    gotAudio[0] = true;
                    phase = Phase.BUFFERING;
                }
                return stream.write(data, length);
            });
            decoderDone = !stopped;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            if (!stopped) {
                LOGGER.debug("DJ mixer decoder stopped", e);
            }
        } finally {
            stream.end();
            process.destroyForcibly();
        }
        if (!stopped && !gotAudio[0]) {
            try {
                process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            fail(MediaTools.lastError(process, webm ? "This song has no audio" : "No Opus audio for this song"));
        }
    }

    private void fail(String message) {
        LOGGER.warn("DJ mixer could not play {} ({}): {}", track.title(), track.id(), message);
        error = message;
        phase = Phase.FAILED;
    }

    /** Main thread, every tick. {@code clock} is the mixer's current position in the song. */
    void tick(LongSupplier clock, float volume) {
        if (stopped) {
            return;
        }
        // A decoder that ended with nothing is left for run() to report as failed
        if (phase == Phase.BUFFERING && (stream.buffered() >= PREBUFFER || (stream.ended() && stream.buffered() > 0))) {
            // Whatever time went by while resolving and buffering is cut off the front, so this client lines up
            long late = clock.getAsLong() - offsetMs;
            if (late > 0L) {
                stream.skip(late * PcmStream.BYTES_PER_SECOND / 1000L);
            }
            sound = new MixerSound(stream, volume);
            Minecraft.getInstance().getSoundManager().play(sound);
            phase = Phase.PLAYING;
        } else if (phase == Phase.PLAYING && sound != null) {
            sound.setTargetVolume(volume);
            if (!Minecraft.getInstance().getSoundManager().isActive(sound)) {
                phase = Phase.ENDED;
            }
        }
    }

    void stop() {
        stopped = true;
        stream.close();
        Process process = download;
        if (process != null) {
            process.destroyForcibly();
        }
        if (sound != null) {
            sound.finish();
            Minecraft.getInstance().getSoundManager().stop(sound);
        }
    }

    Phase phase() {
        return phase;
    }

    String error() {
        return error;
    }

    /** The sound ended without the song being over (sounds reloaded with F3+T, or the stream died): start again. */
    boolean droppedOut() {
        return phase == Phase.ENDED && !decoderDone;
    }
}
