package ch.rere232.skofcraft.client.audio;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.audio.MixerTrack;
import ch.rere232.skofcraft.config.SkofcraftClientConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Finds, installs and runs the programs the DJ mixer needs on this computer: yt-dlp to talk to YouTube, and Deno,
 * which yt-dlp uses to solve YouTube's JavaScript challenges. The audio itself is decoded in Java by
 * {@link WebmOpusDecoder}. The tools are looked for in {@code .minecraft/skofcraft/media} first, then on the PATH,
 * and downloaded there the first time they are needed unless the client config says not to.
 */
@OnlyIn(Dist.CLIENT)
public final class MediaTools {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SEARCH_RESULTS = 20;
    /** yt-dlp learnt --js-runtimes in this release; older ones would refuse the flag. */
    private static final String JS_RUNTIME_VERSION = "2025.11";

    public enum State { UNKNOWN, CHECKING, READY, MISSING, INSTALLING, FAILED }

    public record StreamSource(String url, Map<String, String> headers, long durationMs, String title) {}

    private static final AtomicInteger THREADS = new AtomicInteger();
    static final ExecutorService WORKER = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "SKOFCRAFT DJ Mixer " + THREADS.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });
    /** Length lookups for songs a listing gave no length for: a few at a time, and only so many per batch. */
    private static final int MAX_DURATION_LOOKUPS = 25;
    private static final ExecutorService LOOKUPS = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "SKOFCRAFT DJ Mixer lookup " + THREADS.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });
    private static final Set<Process> PROCESSES = ConcurrentHashMap.newKeySet();
    private static final Map<Process, StringBuilder> ERRORS = new ConcurrentHashMap<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> PROCESSES.forEach(Process::destroyForcibly), "SKOFCRAFT DJ Mixer cleanup"));
    }

    private static volatile State state = State.UNKNOWN;
    private static volatile String message = "";
    private static volatile double progress;
    @Nullable private static volatile String ytDlp;
    @Nullable private static volatile String deno;
    private static volatile String ytDlpVersion = "";

    private MediaTools() {}

    public static State state() {
        return state;
    }

    public static boolean ready() {
        return state == State.READY;
    }

    /** What the tools are doing, for the screen. */
    public static String message() {
        return message;
    }

    /** Download progress from 0 to 1 while installing. */
    public static double progress() {
        return progress;
    }

    public static boolean hasYtDlp() {
        return ytDlp != null;
    }

    public static boolean hasDeno() {
        return deno != null;
    }

    public static Path binDir() {
        return FMLPaths.GAMEDIR.get().resolve(SkofcraftMod.MODID).resolve("media");
    }

    public static void ensureChecked() {
        if (state == State.UNKNOWN) {
            recheck();
        }
    }

    public static void recheck() {
        if (state == State.CHECKING || state == State.INSTALLING) {
            return;
        }
        state = State.CHECKING;
        message = "Looking for yt-dlp...";
        WORKER.submit(MediaTools::check);
    }

    private static void check() {
        ytDlp = locate("yt-dlp", "--version");
        deno = locate("deno", "--version");
        if (ytDlp != null) {
            Result version = run(List.of(ytDlp, "--version"), 20);
            ytDlpVersion = version.out().trim();
        }
        if (ytDlp != null) {
            state = State.READY;
            message = "yt-dlp " + ytDlpVersion + (deno == null ? " (no Deno: some songs may not play)" : "");
        } else {
            state = State.MISSING;
            message = "Missing: yt-dlp";
            if (SkofcraftClientConfig.autoDownloadMediaTools()) {
                install();
            }
        }
    }

    @Nullable
    private static String locate(String name, String versionFlag) {
        Path local = binDir().resolve(executable(name));
        if (Files.isRegularFile(local) && run(List.of(local.toString(), versionFlag), 20).exit() == 0) {
            return local.toString();
        }
        return run(List.of(name, versionFlag), 20).exit() == 0 ? name : null;
    }

    private static String executable(String name) {
        return os() == Os.WINDOWS ? name + ".exe" : name;
    }

    // ---- installing ----

    public static void install() {
        if (state == State.CHECKING || state == State.INSTALLING) {
            return;
        }
        state = State.INSTALLING;
        progress = 0.0D;
        WORKER.submit(() -> {
            try {
                Files.createDirectories(binDir());
                if (ytDlp == null) {
                    message = "Downloading yt-dlp...";
                    Path target = binDir().resolve(executable("yt-dlp"));
                    download(ytDlpUrl(), target);
                    markExecutable(target);
                }
                if (deno == null) {
                    message = "Downloading Deno...";
                    extractFromZip(download(denoUrl(), binDir().resolve("deno.zip")), executable("deno"));
                }
                state = State.UNKNOWN;
                recheck();
            } catch (Exception e) {
                LOGGER.warn("Could not install the DJ mixer tools", e);
                message = "Install failed: " + e.getMessage();
                state = State.FAILED;
            }
        });
    }

    /** Asks yt-dlp to update itself; YouTube changes often and an old yt-dlp is the usual reason songs stop playing. */
    public static void updateYtDlp() {
        String tool = ytDlp;
        if (tool == null || state == State.INSTALLING || state == State.CHECKING) {
            return;
        }
        state = State.INSTALLING;
        message = "Updating yt-dlp...";
        progress = 0.0D;
        WORKER.submit(() -> {
            Result result = run(List.of(tool, "-U"), 180);
            state = State.UNKNOWN;
            recheck();
            if (result.exit() != 0) {
                LOGGER.warn("yt-dlp -U failed: {}", result.err());
            }
        });
    }

    private static Path download(String url, Path target) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(20)).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).header("User-Agent", "SKOFCRAFT DJ Mixer").GET().build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            response.body().close();
            throw new IOException("HTTP " + response.statusCode() + " for " + url);
        }
        long total = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
        Path part = target.resolveSibling(target.getFileName() + ".part");
        try (InputStream in = response.body(); OutputStream out = Files.newOutputStream(part)) {
            byte[] buffer = new byte[64 * 1024];
            long done = 0L;
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
                done += read;
                progress = total > 0L ? (double) done / total : 0.0D;
            }
        }
        Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    /** Pulls the one executable called {@code name} out of a downloaded zip, wherever it is inside, and deletes the zip. */
    private static void extractFromZip(Path zip, String name) throws IOException {
        Path target = binDir().resolve(name);
        boolean found = false;
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                String entryName = entry.getName().replace('\\', '/');
                if (!entry.isDirectory() && (entryName.equals(name) || entryName.endsWith("/" + name))) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    found = true;
                    break;
                }
            }
        } finally {
            Files.deleteIfExists(zip);
        }
        if (!found) {
            throw new IOException(name + " was not in the download");
        }
        markExecutable(target);
    }

    private static void markExecutable(Path file) {
        if (!file.toFile().setExecutable(true)) {
            LOGGER.debug("Could not mark {} executable", file);
        }
    }

    private enum Os { WINDOWS, MAC, LINUX }

    private static Os os() {
        String name = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return name.contains("win") ? Os.WINDOWS : name.contains("mac") ? Os.MAC : Os.LINUX;
    }

    private static boolean arm() {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        return arch.contains("aarch64") || arch.contains("arm64");
    }

    private static String ytDlpUrl() {
        String base = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/";
        return switch (os()) {
            case WINDOWS -> base + "yt-dlp.exe";
            case MAC -> base + "yt-dlp_macos";
            case LINUX -> base + (arm() ? "yt-dlp_linux_aarch64" : "yt-dlp_linux");
        };
    }

    private static String denoUrl() {
        String base = "https://github.com/denoland/deno/releases/latest/download/deno-";
        String arch = arm() ? "aarch64" : "x86_64";
        return switch (os()) {
            case WINDOWS -> base + "x86_64-pc-windows-msvc.zip";
            case MAC -> base + arch + "-apple-darwin.zip";
            case LINUX -> base + arch + "-unknown-linux-gnu.zip";
        };
    }

    // ---- talking to YouTube ----

    private static List<String> ytDlpCommand(String... args) {
        List<String> command = new ArrayList<>();
        command.add(ytDlp);
        command.add("--ignore-config");
        command.add("--no-warnings");
        command.add("--no-progress");
        // yt-dlp finds a Deno on the PATH by itself; only the one in our own folder has to be pointed out
        if (deno != null && !"deno".equals(deno) && ytDlpVersion.compareTo(JS_RUNTIME_VERSION) >= 0) {
            command.add("--js-runtimes");
            command.add("deno:" + deno);
        }
        command.addAll(List.of(args));
        return command;
    }

    /** Is this something to hand to yt-dlp as a link rather than to search for? Only YouTube links are accepted. */
    public static boolean isYouTubeUrl(String text) {
        try {
            URI uri = URI.create(text.trim());
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            return ("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                    && (host.equals("youtu.be") || host.equals("youtube.com") || host.endsWith(".youtube.com"));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Searches YouTube, or YouTube Music's song results when {@code music} is set. */
    public static CompletableFuture<List<MixerTrack>> search(String query, boolean music) {
        String target = music
                ? "https://music.youtube.com/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "#songs"
                : "ytsearch" + SEARCH_RESULTS + ":" + query;
        return listing(target, false);
    }

    /** A video, YouTube Music song or playlist link, as the songs it holds. */
    public static CompletableFuture<List<MixerTrack>> resolveLink(String url) {
        boolean playlist = url.contains("/playlist") || url.contains("/browse/");
        return listing(url.trim(), !playlist);
    }

    private static CompletableFuture<List<MixerTrack>> listing(String target, boolean singleVideo) {
        return CompletableFuture.supplyAsync(() -> {
            requireReady();
            List<String> command = ytDlpCommand("--flat-playlist", "--dump-single-json");
            if (singleVideo) {
                command.add("--no-playlist");
            }
            command.add("--");
            command.add(target);
            Result result = run(command, 60);
            if (result.exit() != 0) {
                throw new IllegalStateException(lastLine(result.err(), "yt-dlp could not read that"));
            }
            JsonObject root = JsonParser.parseString(result.out()).getAsJsonObject();
            List<MixerTrack> tracks = new ArrayList<>();
            if (root.has("entries") && root.get("entries").isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray("entries")) {
                    if (element.isJsonObject()) {
                        MixerTrack track = trackFrom(element.getAsJsonObject());
                        if (track != null) {
                            tracks.add(track);
                        }
                    }
                }
            } else {
                MixerTrack track = trackFrom(root);
                if (track != null) {
                    tracks.add(track);
                }
            }
            return tracks;
        }, WORKER);
    }

    /** Looks up the songs a listing left without a length, since the mixer needs it to know when to move on. */
    public static CompletableFuture<List<MixerTrack>> withDurations(List<MixerTrack> tracks) {
        List<CompletableFuture<MixerTrack>> lookups = new ArrayList<>();
        int missing = 0;
        for (MixerTrack track : tracks) {
            if (track.durationMs() > 0L) {
                lookups.add(CompletableFuture.completedFuture(track));
            } else if (missing++ < MAX_DURATION_LOOKUPS) {
                lookups.add(resolveStream(track, LOOKUPS).handle((source, error) -> source == null || source.durationMs() <= 0L ? null
                        : MixerTrack.sanitized(track.id(), track.title(), track.artist(), source.durationMs())));
            }
        }
        return CompletableFuture.allOf(lookups.toArray(CompletableFuture[]::new)).thenApply(ignored -> {
            List<MixerTrack> done = new ArrayList<>();
            for (CompletableFuture<MixerTrack> lookup : lookups) {
                MixerTrack track = lookup.join();
                if (track != null) {
                    done.add(track);
                }
            }
            return done;
        });
    }

    /** The direct audio URL of a song, with the HTTP headers YouTube expects when fetching it. */
    public static CompletableFuture<StreamSource> resolveStream(MixerTrack track) {
        return resolveStream(track, WORKER);
    }

    private static CompletableFuture<StreamSource> resolveStream(MixerTrack track, ExecutorService executor) {
        return CompletableFuture.supplyAsync(() -> {
            requireReady();
            Result result = run(ytDlpCommand("-f", "bestaudio/best", "--no-playlist", "--dump-single-json", "--", track.watchUrl()), 60);
            if (result.exit() != 0) {
                throw new IllegalStateException(lastLine(result.err(), "yt-dlp could not get this song"));
            }
            JsonObject root = JsonParser.parseString(result.out()).getAsJsonObject();
            String url = string(root, "url");
            if (url.isEmpty()) {
                throw new IllegalStateException("No audio stream found");
            }
            Map<String, String> headers = new LinkedHashMap<>();
            if (root.has("http_headers") && root.get("http_headers").isJsonObject()) {
                for (Map.Entry<String, JsonElement> header : root.getAsJsonObject("http_headers").entrySet()) {
                    if (header.getValue().isJsonPrimitive()) {
                        headers.put(header.getKey(), header.getValue().getAsString());
                    }
                }
            }
            return new StreamSource(url, headers, durationOf(root), string(root, "title"));
        }, executor);
    }

    /**
     * Starts yt-dlp writing the song's WebM/Opus audio to its stdout. YouTube serves almost every video's audio as
     * Opus; the rare one without it fails with a clear message. stderr is kept for the error, see {@link #lastError}.
     */
    static Process startAudioDownload(MixerTrack track) throws IOException {
        requireReady();
        List<String> command = ytDlpCommand("-f", "bestaudio[acodec=opus]/bestaudio[ext=webm]", "--no-playlist",
                "--no-part", "--quiet", "-o", "-", "--", track.watchUrl());
        Process process = new ProcessBuilder(command).start();
        PROCESSES.add(process);
        process.onExit().thenRun(() -> PROCESSES.remove(process));
        process.getOutputStream().close();
        StringBuilder errors = new StringBuilder();
        ERRORS.put(process, errors);
        WORKER.submit(() -> {
            String err = readAll(process.getErrorStream());
            synchronized (errors) {
                errors.append(err);
            }
        });
        process.onExit().thenRun(() -> CompletableFuture.delayedExecutor(30, TimeUnit.SECONDS).execute(() -> ERRORS.remove(process)));
        return process;
    }

    /** The last thing yt-dlp complained about for a download started with {@link #startAudioDownload}. */
    static String lastError(Process process, String fallback) {
        StringBuilder errors = ERRORS.get(process);
        if (errors == null) {
            return fallback;
        }
        synchronized (errors) {
            return lastLine(errors.toString(), fallback);
        }
    }

    private static void requireReady() {
        if (!ready()) {
            throw new IllegalStateException("yt-dlp is not set up");
        }
    }

    @Nullable
    private static MixerTrack trackFrom(JsonObject json) {
        String id = string(json, "id");
        if (!MixerTrack.isValidId(id)) {
            return null;
        }
        String artist = string(json, "artist");
        if (artist.isEmpty() && json.has("artists") && json.get("artists").isJsonArray()) {
            JsonArray artists = json.getAsJsonArray("artists");
            if (!artists.isEmpty() && artists.get(0).isJsonPrimitive()) {
                artist = artists.get(0).getAsString();
            }
        }
        for (String field : new String[] {"channel", "uploader", "creator"}) {
            if (artist.isEmpty()) {
                artist = string(json, field);
            }
        }
        return MixerTrack.sanitized(id, string(json, "title"), artist, durationOf(json));
    }

    private static long durationOf(JsonObject json) {
        JsonElement duration = json.get("duration");
        if (duration != null && duration.isJsonPrimitive() && duration.getAsJsonPrimitive().isNumber()) {
            return Math.round(duration.getAsDouble() * 1000.0D);
        }
        return 0L;
    }

    private static String string(JsonObject json, String field) {
        JsonElement value = json.get(field);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
    }

    private static String lastLine(String text, String fallback) {
        String[] lines = text.trim().split("\\R");
        String last = lines.length == 0 ? "" : lines[lines.length - 1].trim();
        return last.isEmpty() ? fallback : last.replaceFirst("^ERROR:\\s*", "");
    }

    // ---- running programs ----

    record Result(int exit, String out, String err) {}

    private static Result run(List<String> command, int timeoutSeconds) {
        Process process;
        try {
            process = new ProcessBuilder(command).start();
        } catch (IOException e) {
            return new Result(-1, "", e.getMessage() == null ? "" : e.getMessage());
        }
        PROCESSES.add(process);
        // Reading stdout blocks until the program closes it, so the timeout has to kill it from elsewhere
        CompletableFuture.delayedExecutor(timeoutSeconds, TimeUnit.SECONDS, WORKER).execute(() -> {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        });
        try {
            process.getOutputStream().close();
            CompletableFuture<String> err = CompletableFuture.supplyAsync(() -> readAll(process.getErrorStream()), WORKER);
            String out = readAll(process.getInputStream());
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return new Result(-1, out, "Timed out");
            }
            return new Result(process.exitValue(), out, err.get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            process.destroyForcibly();
            return new Result(-1, "", String.valueOf(e.getMessage()));
        } finally {
            PROCESSES.remove(process);
        }
    }

    private static String readAll(InputStream in) {
        try (in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            in.transferTo(out);
            return out.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
}
