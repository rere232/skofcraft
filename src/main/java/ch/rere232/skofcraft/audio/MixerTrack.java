package ch.rere232.skofcraft.audio;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * One song in a DJ mixer queue. Only the YouTube video id travels between server and clients: every client builds the
 * watch URL itself, so nobody can make other players' yt-dlp open an arbitrary address.
 */
public record MixerTrack(String id, String title, String artist, long durationMs) {
    public static final int MAX_TITLE = 120;
    public static final int MAX_ARTIST = 60;
    /** Twelve hours: anything longer is a livestream or a lie, and the server would never move to the next track. */
    public static final long MAX_DURATION_MS = 12L * 60L * 60L * 1000L;
    private static final Pattern VIDEO_ID = Pattern.compile("[A-Za-z0-9_-]{11}");

    public static boolean isValidId(@Nullable String id) {
        return id != null && VIDEO_ID.matcher(id).matches();
    }

    /** Cleans up whatever a client sent; null when the id is not a YouTube video id. */
    @Nullable
    public static MixerTrack sanitized(String id, String title, String artist, long durationMs) {
        if (!isValidId(id)) {
            return null;
        }
        String cleanTitle = clip(title == null || title.isBlank() ? id : title, MAX_TITLE);
        String cleanArtist = clip(artist == null ? "" : artist, MAX_ARTIST);
        return new MixerTrack(id, cleanTitle, cleanArtist, Math.max(0L, Math.min(MAX_DURATION_MS, durationMs)));
    }

    private static String clip(String text, int max) {
        String flat = text.replaceAll("[\\p{Cntrl}]", " ").trim();
        return flat.length() > max ? flat.substring(0, max) : flat;
    }

    public String watchUrl() {
        return "https://www.youtube.com/watch?v=" + id;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id);
        tag.putString("Title", title);
        tag.putString("Artist", artist);
        tag.putLong("Duration", durationMs);
        return tag;
    }

    @Nullable
    public static MixerTrack load(CompoundTag tag) {
        return sanitized(tag.getString("Id"), tag.getString("Title"), tag.getString("Artist"), tag.getLong("Duration"));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(id, 16);
        buf.writeUtf(title, MAX_TITLE * 4);
        buf.writeUtf(artist, MAX_ARTIST * 4);
        buf.writeVarLong(durationMs);
    }

    @Nullable
    public static MixerTrack read(FriendlyByteBuf buf) {
        return sanitized(buf.readUtf(16), buf.readUtf(MAX_TITLE * 4), buf.readUtf(MAX_ARTIST * 4), buf.readVarLong());
    }

    public static String formatTime(long ms) {
        long total = Math.max(0L, ms) / 1000L;
        long hours = total / 3600L;
        long minutes = (total / 60L) % 60L;
        long seconds = total % 60L;
        return hours > 0 ? String.format("%d:%02d:%02d", hours, minutes, seconds) : String.format("%d:%02d", minutes, seconds);
    }
}
