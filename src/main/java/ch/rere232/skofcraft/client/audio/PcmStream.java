package ch.rere232.skofcraft.client.audio;

import net.minecraft.client.sounds.AudioStream;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;

/**
 * Decoded PCM, buffered between the thread reading the decoder and Minecraft's sound thread. The sound
 * thread must never wait, so when the network falls behind it gets silence instead and the song carries on.
 */
@OnlyIn(Dist.CLIENT)
public final class PcmStream implements AudioStream {
    public static final int SAMPLE_RATE = 48000;
    private static final int FRAME = 4;
    public static final int BYTES_PER_SECOND = SAMPLE_RATE * FRAME;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 2, true, false);
    private static final int CAPACITY = BYTES_PER_SECOND * 30;

    private final byte[] ring = new byte[CAPACITY];
    private int head;
    private int size;
    private boolean ended;
    private boolean closed;

    @Override
    public AudioFormat getFormat() {
        return FORMAT;
    }

    /** Called by the decoder thread; waits while the buffer is full. Returns false once the stream was closed. */
    public synchronized boolean write(byte[] data, int length) throws InterruptedException {
        int offset = 0;
        while (offset < length) {
            while (size == CAPACITY && !closed) {
                wait(250L);
            }
            if (closed) {
                return false;
            }
            int tail = (head + size) % CAPACITY;
            int chunk = Math.min(length - offset, Math.min(CAPACITY - size, CAPACITY - tail));
            System.arraycopy(data, offset, ring, tail, chunk);
            size += chunk;
            offset += chunk;
        }
        return true;
    }

    /** The decoder has nothing more to give. */
    public synchronized void end() {
        ended = true;
    }

    public synchronized boolean ended() {
        return ended;
    }

    public synchronized int buffered() {
        return size;
    }

    /** Drops up to {@code bytes} from the front, to catch up with the mixer's clock. */
    public synchronized void skip(long bytes) {
        int drop = (int) Math.min(size, Math.max(0L, bytes)) / FRAME * FRAME;
        head = (head + drop) % CAPACITY;
        size -= drop;
        notifyAll();
    }

    @Override
    public synchronized ByteBuffer read(int length) {
        int wanted = Math.max(FRAME, length / FRAME * FRAME);
        if (closed || (ended && size < FRAME)) {
            return null;
        }
        int available = Math.min(wanted, size / FRAME * FRAME);
        ByteBuffer out = BufferUtils.createByteBuffer(ended ? available : wanted);
        int copied = 0;
        while (copied < available) {
            int chunk = Math.min(available - copied, CAPACITY - head);
            out.put(ring, head, chunk);
            head = (head + chunk) % CAPACITY;
            copied += chunk;
        }
        size -= available;
        // Underrun: pad with silence (the buffer starts zeroed) so the source keeps playing
        out.position(out.capacity());
        out.flip();
        notifyAll();
        return out;
    }

    @Override
    public synchronized void close() {
        closed = true;
        notifyAll();
    }
}
