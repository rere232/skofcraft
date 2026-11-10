package ch.rere232.skofcraft.client.audio;

import ch.rere232.skofcraft.audio.MixerTrack;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Songs saved on disk as yt-dlp downloads them, in {@code .minecraft/skofcraft/media/cache}. Asking YouTube for a
 * song takes yt-dlp several seconds; with the file at hand, replays, jumps, resumes and the next song in the queue
 * (fetched ahead of time) start at once.
 *
 * <p>A download keeps going when the session that started it stops (a seek, a pause), and readers follow the file
 * as it grows. A finished song is {@code <id>.webm} next to an empty {@code <id>.done}; a webm without its marker is
 * a download that was cut off and gets thrown away.
 */
@OnlyIn(Dist.CLIENT)
final class TrackCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long MAX_BYTES = 512L * 1024L * 1024L;
    /** Background fetches at once, on top of whatever is playing. */
    private static final int MAX_PREFETCHES = 2;

    private static final Map<String, Download> ACTIVE = new ConcurrentHashMap<>();
    private static volatile boolean cleaned;

    private TrackCache() {}

    static Path dir() {
        return MediaTools.binDir().resolve("cache");
    }

    private static Path audio(String id) {
        return dir().resolve(id + ".webm");
    }

    private static Path marker(String id) {
        return dir().resolve(id + ".done");
    }

    static boolean isCached(MixerTrack track) {
        return Files.exists(marker(track.id())) && Files.exists(audio(track.id()));
    }

    /** The song's audio from the start, from disk if it is there, otherwise while it downloads. */
    static InputStream open(MixerTrack track) throws IOException {
        cleanOnce();
        if (isCached(track)) {
            touch(track.id());
            return Files.newInputStream(audio(track.id()));
        }
        Download download = start(track);
        return new Follower(download);
    }

    /** Starts fetching a song nobody is listening to yet, so it is ready when its turn comes. */
    static void prefetch(MixerTrack track) {
        if (!MediaTools.ready() || isCached(track) || ACTIVE.containsKey(track.id())) {
            return;
        }
        long background = ACTIVE.values().stream().filter(d -> d.prefetch).count();
        if (background >= MAX_PREFETCHES) {
            return;
        }
        try {
            cleanOnce();
            Download download = start(track);
            download.prefetch = true;
        } catch (IOException e) {
            LOGGER.debug("DJ mixer could not prefetch {}", track.id(), e);
        }
    }

    private static synchronized Download start(MixerTrack track) throws IOException {
        Download existing = ACTIVE.get(track.id());
        if (existing != null) {
            existing.prefetch = false;
            return existing;
        }
        Files.createDirectories(dir());
        Files.deleteIfExists(marker(track.id()));
        Download download = new Download(track, MediaTools.startAudioDownload(track));
        ACTIVE.put(track.id(), download);
        MediaTools.WORKER.submit(download::run);
        return download;
    }

    private static void touch(String id) {
        if (!marker(id).toFile().setLastModified(System.currentTimeMillis())) {
            LOGGER.debug("Could not touch {}", marker(id));
        }
    }

    /** First use in a session: drop half-downloaded songs from last time. */
    private static void cleanOnce() {
        if (cleaned) {
            return;
        }
        cleaned = true;
        File[] files = dir().toFile().listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            String name = file.getName();
            if (name.endsWith(".webm") && !new File(file.getParentFile(), name.replace(".webm", ".done")).exists()) {
                file.delete();
            }
        }
    }

    /** Keeps the cache under {@link #MAX_BYTES}, deleting the songs played longest ago. */
    private static void evict() {
        File[] markers = dir().toFile().listFiles((d, name) -> name.endsWith(".done"));
        if (markers == null) {
            return;
        }
        Arrays.sort(markers, Comparator.comparingLong(File::lastModified));
        long total = 0L;
        for (File m : markers) {
            total += new File(m.getParentFile(), m.getName().replace(".done", ".webm")).length();
        }
        for (File m : markers) {
            if (total <= MAX_BYTES) {
                break;
            }
            String id = m.getName().replace(".done", "");
            if (ACTIVE.containsKey(id)) {
                continue;
            }
            File webm = new File(m.getParentFile(), id + ".webm");
            total -= webm.length();
            m.delete();
            webm.delete();
        }
    }

    /** One yt-dlp download writing a song to its cache file. */
    private static final class Download {
        final MixerTrack track;
        final Process process;
        volatile long written;
        volatile boolean finished;
        volatile String error;
        volatile boolean prefetch;

        Download(MixerTrack track, Process process) {
            this.track = track;
            this.process = process;
        }

        void run() {
            Path file = audio(track.id());
            byte[] buffer = new byte[64 * 1024];
            try (InputStream in = process.getInputStream(); OutputStream out = Files.newOutputStream(file)) {
                int read;
                while ((read = in.read(buffer)) >= 0) {
                    out.write(buffer, 0, read);
                    out.flush();
                    written += read;
                }
                process.waitFor();
                if (process.exitValue() != 0 || written == 0L) {
                    throw new IOException(MediaTools.lastError(process, "yt-dlp could not download this song"));
                }
                Files.createFile(marker(track.id()));
            } catch (Exception e) {
                error = e.getMessage() == null ? e.toString() : e.getMessage();
                LOGGER.warn("DJ mixer download of {} ({}) failed: {}", track.title(), track.id(), error);
            } finally {
                finished = true;
                process.destroyForcibly();
                ACTIVE.remove(track.id());
                if (error != null) {
                    try {
                        Files.deleteIfExists(file);
                    } catch (IOException ignored) {
                        // in use by a reader; cleanOnce gets it next time
                    }
                } else {
                    evict();
                }
            }
        }
    }

    /** Reads a cache file while its download is still writing it, waiting for more instead of ending early. */
    private static final class Follower extends InputStream {
        private final Download download;
        private RandomAccessFile file;
        private long position;

        Follower(Download download) {
            this.download = download;
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            return read(one, 0, 1) < 0 ? -1 : one[0] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            while (true) {
                boolean finished = download.finished;
                long available = download.written - position;
                if (available > 0L) {
                    if (file == null) {
                        file = new RandomAccessFile(audio(download.track.id()).toFile(), "r");
                    }
                    file.seek(position);
                    int read = file.read(b, off, (int) Math.min(len, available));
                    if (read > 0) {
                        position += read;
                        return read;
                    }
                }
                if (finished) {
                    if (download.error != null) {
                        throw new IOException(download.error);
                    }
                    return -1;
                }
                try {
                    Thread.sleep(20L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted", e);
                }
            }
        }

        @Override
        public void close() throws IOException {
            if (file != null) {
                file.close();
            }
        }
    }
}
