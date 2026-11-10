package ch.rere232.skofcraft.client.audio;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Finds the kicks and bass hits in a song as it is decoded, for the screen shake. The audio is low-passed to keep
 * only the bass, cut into 10 ms frames, and a hit is a frame whose bass is well above the last half second's.
 *
 * <p>Hits are stamped with their time in the song, not the time they were decoded: the decoder runs seconds ahead
 * of what the speakers play, so the shake has to wait for the mixer's clock to get there.
 */
@OnlyIn(Dist.CLIENT)
final class BassDetector {
    private static final int FRAME_SAMPLES = PcmStream.SAMPLE_RATE / 100;
    private static final int FRAME_MS = 10;
    /** One-pole low-pass at about 120 Hz, applied twice for a steeper cut. */
    private static final float ALPHA = (float) (1.0D - Math.exp(-2.0D * Math.PI * 120.0D / PcmStream.SAMPLE_RATE));
    /** How fast the "normal" bass level follows the music: about half a second. */
    private static final double AVERAGE_RATE = 0.02D;
    private static final double ONSET_RATIO = 1.45D;
    private static final double MIN_LEVEL = 0.03D;
    private static final double MIN_RISE = 0.012D;
    /** No two hits closer than this, so one long bass note is one shake. */
    private static final long REFRACTORY_MS = 170L;

    record Hit(long timeMs, float strength) {}

    private final ConcurrentLinkedQueue<Hit> hits = new ConcurrentLinkedQueue<>();
    private final long startMs;
    private float low1;
    private float low2;
    private double energy;
    private int samples;
    private long frames;
    private double average;
    private double previous;
    private long lastHitMs = Long.MIN_VALUE / 2;

    BassDetector(long startMs) {
        this.startMs = startMs;
    }

    /** Decoder thread: 16-bit little-endian stereo PCM, the same bytes that go to the speakers. */
    void feed(byte[] data, int length) {
        for (int i = 0; i + 3 < length; i += 4) {
            int left = (short) ((data[i] & 0xFF) | data[i + 1] << 8);
            int right = (short) ((data[i + 2] & 0xFF) | data[i + 3] << 8);
            float mono = (left + right) / 65536.0F;
            low1 += ALPHA * (mono - low1);
            low2 += ALPHA * (low1 - low2);
            energy += low2 * low2;
            if (++samples == FRAME_SAMPLES) {
                endFrame();
            }
        }
    }

    private void endFrame() {
        double level = Math.sqrt(energy / samples);
        energy = 0.0D;
        samples = 0;
        long time = startMs + frames++ * FRAME_MS;
        if (average == 0.0D) {
            average = level;
        }
        if (level > average * ONSET_RATIO && level > MIN_LEVEL && level - previous > MIN_RISE && time - lastHitMs >= REFRACTORY_MS) {
            float strength = (float) Math.max(0.35D, Math.min(1.0D, 0.35D + (level / average - ONSET_RATIO) * 0.5D));
            hits.add(new Hit(time, strength));
            lastHitMs = time;
        }
        average += (level - average) * AVERAGE_RATE;
        previous = level;
    }

    /** The strongest hit that is due by {@code nowMs}, or 0; hits older than that are used up. */
    float poll(long nowMs) {
        float strongest = 0.0F;
        Hit hit;
        while ((hit = hits.peek()) != null && hit.timeMs() <= nowMs) {
            hits.poll();
            // A hit from long ago (the stream fell behind, or the game was paused) is dropped silently
            if (nowMs - hit.timeMs() < 150L) {
                strongest = Math.max(strongest, hit.strength());
            }
        }
        return strongest;
    }
}
