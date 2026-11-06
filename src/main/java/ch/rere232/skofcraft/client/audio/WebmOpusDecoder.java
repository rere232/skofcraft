package ch.rere232.skofcraft.client.audio;

import io.github.jaredmdobson.concentus.OpusDecoder;
import io.github.jaredmdobson.concentus.OpusException;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Turns YouTube's WebM/Opus audio into 48 kHz 16-bit stereo PCM, all in Java so nobody needs ffmpeg. It reads the
 * WebM (Matroska) container as a stream, element by element, and hands each Opus packet to Concentus.
 *
 * <p>Seeking is done by reading from the start and throwing away whatever comes before {@code offsetMs}: skipped
 * packets are not even decoded, so it costs little more than the download of the audio up to that point.
 */
@OnlyIn(Dist.CLIENT)
final class WebmOpusDecoder {
    private static final int SEGMENT = 0x18538067;
    private static final int INFO = 0x1549A966;
    private static final int TIMECODE_SCALE = 0x2AD7B1;
    private static final int TRACKS = 0x1654AE6B;
    private static final int TRACK_ENTRY = 0xAE;
    private static final int TRACK_NUMBER = 0xD7;
    private static final int CODEC_ID = 0x86;
    private static final int CODEC_PRIVATE = 0x63A2;
    private static final int CLUSTER = 0x1F43B675;
    private static final int CLUSTER_TIMECODE = 0xE7;
    private static final int SIMPLE_BLOCK = 0xA3;
    private static final int BLOCK_GROUP = 0xA0;
    private static final int BLOCK = 0xA1;

    private static final int MAX_ELEMENT = 4 * 1024 * 1024;
    /** Opus needs a few packets before the target to settle, or the first sound after a jump comes out mangled. */
    private static final long PRE_ROLL_MS = 120L;
    private static final int MAX_FRAME = 5760;

    /** Where decoded audio goes; returning false stops decoding. */
    interface Sink {
        boolean write(byte[] data, int length) throws InterruptedException;
    }

    private final DataInputStream in;
    private final long offsetMs;
    private final Sink sink;

    private long timecodeScale = 1_000_000L;
    private long clusterTime;
    private long opusTrack = -1L;
    private long pendingTrackNumber = -1L;
    private String pendingCodec = "";
    private int channels = 2;
    private int preSkip;
    private long samplesOut;
    private OpusDecoder decoder;
    private final short[] pcm = new short[MAX_FRAME * 2];
    private final byte[] out = new byte[MAX_FRAME * 4];

    private WebmOpusDecoder(InputStream in, long offsetMs, Sink sink) {
        this.in = new DataInputStream(new BufferedInputStream(in, 64 * 1024));
        this.offsetMs = Math.max(0L, offsetMs);
        this.sink = sink;
    }

    /** Decodes {@code in} until it ends or {@code sink} refuses more; returns false if it was not WebM/Opus at all. */
    static boolean decode(InputStream in, long offsetMs, Sink sink) throws IOException, InterruptedException {
        return new WebmOpusDecoder(in, offsetMs, sink).run();
    }

    private boolean run() throws IOException, InterruptedException {
        try {
            while (true) {
                int id = readId();
                long size = readSize();
                switch (id) {
                    // Containers: carry on reading their children in line, whatever their size says
                    case SEGMENT, INFO, TRACKS, CLUSTER, BLOCK_GROUP -> { }
                    case TRACK_ENTRY -> {
                        pendingTrackNumber = -1L;
                        pendingCodec = "";
                    }
                    case TIMECODE_SCALE -> timecodeScale = Math.max(1L, readUnsigned(size));
                    case TRACK_NUMBER -> {
                        pendingTrackNumber = readUnsigned(size);
                        claimTrack();
                    }
                    case CODEC_ID -> {
                        pendingCodec = new String(readBytes(size), java.nio.charset.StandardCharsets.US_ASCII).trim();
                        claimTrack();
                    }
                    case CODEC_PRIVATE -> readOpusHead(readBytes(size));
                    case CLUSTER_TIMECODE -> clusterTime = readUnsigned(size);
                    case SIMPLE_BLOCK, BLOCK -> {
                        if (!block(readBytes(size))) {
                            return true;
                        }
                    }
                    default -> skip(size);
                }
            }
        } catch (EOFException e) {
            return decoder != null;
        }
    }

    /** The first track that is both numbered and Opus is the one to play. */
    private void claimTrack() {
        if (opusTrack < 0L && pendingTrackNumber >= 0L && "A_OPUS".equals(pendingCodec)) {
            opusTrack = pendingTrackNumber;
        }
    }

    /** The OpusHead header in CodecPrivate: channel count and how many samples of codec delay to drop at the start. */
    private void readOpusHead(byte[] head) {
        if (head.length >= 19 && new String(head, 0, 8, java.nio.charset.StandardCharsets.US_ASCII).equals("OpusHead")) {
            channels = Math.max(1, Math.min(2, head[9] & 0xFF));
            preSkip = (head[10] & 0xFF) | (head[11] & 0xFF) << 8;
        }
    }

    private boolean block(byte[] data) throws IOException, InterruptedException {
        if (data.length < 4) {
            return true;
        }
        int first = data[0] & 0xFF;
        int length = Integer.numberOfLeadingZeros(first) - 23;
        if (length < 1 || length > 8 || data.length < length + 3) {
            return true;
        }
        long track = first & (0xFF >> length);
        for (int i = 1; i < length; i++) {
            track = track << 8 | (data[i] & 0xFF);
        }
        if (opusTrack < 0L || track != opusTrack) {
            return true;
        }
        short relative = (short) ((data[length] & 0xFF) << 8 | (data[length + 1] & 0xFF));
        int flags = data[length + 2] & 0xFF;
        if ((flags & 0x06) != 0) {
            return true; // laced blocks: YouTube never sends them for audio
        }
        int start = length + 3;
        long timeMs = (clusterTime + relative) * timecodeScale / 1_000_000L;
        if (timeMs < offsetMs - PRE_ROLL_MS) {
            return true;
        }
        if (decoder == null) {
            try {
                decoder = new OpusDecoder(PcmStream.SAMPLE_RATE, channels);
            } catch (OpusException e) {
                throw new IOException("Opus decoder: " + e.getMessage(), e);
            }
        }
        int samples;
        try {
            samples = decoder.decode(data, start, data.length - start, pcm, 0, MAX_FRAME, false);
        } catch (OpusException e) {
            return true; // one broken packet is a click, not a reason to stop the song
        }
        // Drop the codec delay at the very start, and on a jump everything before the target
        int drop = 0;
        if (offsetMs == 0L) {
            drop = (int) Math.max(0L, Math.min(samples, preSkip - samplesOut));
        } else if (timeMs < offsetMs) {
            drop = (int) Math.min(samples, (offsetMs - timeMs) * PcmStream.SAMPLE_RATE / 1000L);
        }
        samplesOut += samples;
        int bytes = 0;
        for (int i = drop; i < samples; i++) {
            short left = pcm[i * channels];
            short right = channels == 2 ? pcm[i * 2 + 1] : left;
            out[bytes++] = (byte) left;
            out[bytes++] = (byte) (left >> 8);
            out[bytes++] = (byte) right;
            out[bytes++] = (byte) (right >> 8);
        }
        return bytes == 0 || sink.write(out, bytes);
    }

    // ---- EBML ----

    /** An element id keeps its length marker bits, as the spec writes them. */
    private int readId() throws IOException {
        int first = in.readUnsignedByte();
        int length = Integer.numberOfLeadingZeros(first) - 23;
        if (length < 1 || length > 4) {
            throw new IOException("Not a WebM stream");
        }
        int id = first;
        for (int i = 1; i < length; i++) {
            id = id << 8 | in.readUnsignedByte();
        }
        return id;
    }

    /** An element size, or -1 for "unknown", which streamed Segments and Clusters may use. */
    private long readSize() throws IOException {
        int first = in.readUnsignedByte();
        int length = Integer.numberOfLeadingZeros(first) - 23;
        if (length < 1 || length > 8) {
            throw new IOException("Bad WebM element size");
        }
        long value = first & (0xFF >> length);
        boolean allOnes = value == (0xFF >> length);
        for (int i = 1; i < length; i++) {
            int b = in.readUnsignedByte();
            allOnes &= b == 0xFF;
            value = value << 8 | b;
        }
        return allOnes ? -1L : value;
    }

    private long readUnsigned(long size) throws IOException {
        if (size < 0L || size > 8L) {
            throw new IOException("Bad WebM number");
        }
        long value = 0L;
        for (int i = 0; i < size; i++) {
            value = value << 8 | in.readUnsignedByte();
        }
        return value;
    }

    private byte[] readBytes(long size) throws IOException {
        if (size < 0L || size > MAX_ELEMENT) {
            throw new IOException("WebM element too big");
        }
        byte[] data = new byte[(int) size];
        in.readFully(data);
        return data;
    }

    private void skip(long size) throws IOException {
        if (size < 0L) {
            return; // unknown-size element we do not care about: its children get skipped one by one
        }
        in.skipNBytes(size);
    }
}
