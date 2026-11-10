package ch.rere232.skofcraft.blockentity;

import ch.rere232.skofcraft.audio.MixerTrack;
import ch.rere232.skofcraft.block.AudioCableBlock;
import ch.rere232.skofcraft.block.SpeakerBlock;
import ch.rere232.skofcraft.client.audio.MixerAudioClient;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The state of a DJ mixer: the queue, which song is on, and a clock. The server owns all of it and only keeps time;
 * every client streams the song itself and seeks to {@link #elapsedMs()}, so everyone hears the same part of the song.
 */
public class DjMixerBlockEntity extends BlockEntity {
    public static final int MAX_QUEUE = 200;
    public static final int MAX_SPEAKERS = 64;
    private static final int MAX_CABLE = 1024;
    private static final int SCAN_INTERVAL = 20;
    /** A little slack after the song's length, so clients that started late still hear the last notes. */
    private static final long END_GRACE_MS = 1500L;
    /** Pressing previous after this far into a song restarts it instead of going back one. */
    private static final long RESTART_THRESHOLD_MS = 3000L;
    /** The longest the clock waits for a listener to have the song ready (nobody near, or nobody with yt-dlp). */
    private static final long WAIT_TIMEOUT_TICKS = 30L * 20L;

    public enum Repeat {
        OFF, ALL, ONE;

        public static Repeat byId(int id) {
            Repeat[] values = values();
            return id >= 0 && id < values.length ? values[id] : OFF;
        }
    }

    private final List<MixerTrack> queue = new ArrayList<>();
    private final List<BlockPos> speakers = new ArrayList<>();
    private int current = -1;
    private boolean playing;
    private long startTick;
    private long pausedMs;
    private int volume = 80;
    private Repeat repeat = Repeat.OFF;
    private boolean shuffle;
    /** Goes up every time the song (re)starts or jumps, so clients know to restart their stream. */
    private int revision;
    /** True once the last song of the queue finished with repeat off: queueing more then starts the new songs. */
    private boolean atEnd;
    /**
     * Holding the clock at {@link #pausedMs} until a listener reports the song ready: fetching it from YouTube takes
     * seconds, and without the wait everyone would come in that far into the song.
     */
    private boolean waiting;
    private long waitingSince;
    /** With shuffle on, the song after this one, picked in advance so clients can fetch it ahead of time. */
    private int shuffleNext = -1;
    private int ticks;

    public DjMixerBlockEntity(BlockPos pos, BlockState state) {
        super(SkofcraftBlockEntities.DJ_MIXER.get(), pos, state);
    }

    public void serverTick() {
        if (level == null) {
            return;
        }
        if (++ticks % SCAN_INTERVAL == 1) {
            scanNetwork();
        }
        if (playing && waiting && now() - waitingSince >= WAIT_TIMEOUT_TICKS) {
            startClock();
        }
        if (playing) {
            MixerTrack track = currentTrack();
            if (track == null) {
                playing = false;
                pausedMs = 0L;
                changed();
            } else if (track.durationMs() > 0L && elapsedMs() >= track.durationMs() + END_GRACE_MS) {
                advance(true);
            }
        }
    }

    /** Walks the cables from the mixer and collects every speaker on them. */
    private void scanNetwork() {
        if (level == null) {
            return;
        }
        Set<BlockPos> seen = new HashSet<>();
        List<BlockPos> found = new ArrayList<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        seen.add(worldPosition);
        open.add(worldPosition);
        int cables = 0;
        while (!open.isEmpty() && found.size() < MAX_SPEAKERS) {
            BlockPos at = open.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = at.relative(direction);
                if (!seen.add(next) || !level.isLoaded(next)) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                if (state.getBlock() instanceof SpeakerBlock) {
                    found.add(next.immutable());
                } else if (state.getBlock() instanceof AudioCableBlock && cables++ < MAX_CABLE) {
                    open.add(next.immutable());
                }
            }
        }
        if (!found.equals(speakers)) {
            speakers.clear();
            speakers.addAll(found);
            changed();
        }
    }

    // ---- playback controls, all server side ----

    public void addTracks(Collection<MixerTrack> tracks, boolean playNow) {
        int first = queue.size();
        for (MixerTrack track : tracks) {
            if (queue.size() >= MAX_QUEUE) {
                break;
            }
            queue.add(track);
        }
        if (queue.size() == first) {
            return;
        }
        if (playNow || currentTrack() == null || atEnd) {
            playIndex(first);
        } else {
            changed();
        }
    }

    public void playPause() {
        if (currentTrack() == null) {
            if (!queue.isEmpty()) {
                playIndex(0);
            }
            return;
        }
        if (playing) {
            pausedMs = elapsedMs();
            playing = false;
            waiting = false;
        } else {
            playing = true;
            atEnd = false;
            holdAt(pausedMs);
        }
        changed();
    }

    public void stop() {
        if (playing || pausedMs != 0L) {
            playing = false;
            waiting = false;
            pausedMs = 0L;
            changed();
        }
    }

    public void next() {
        advance(false);
    }

    public void previous() {
        if (currentTrack() == null) {
            return;
        }
        if (elapsedMs() > RESTART_THRESHOLD_MS || queue.size() == 1) {
            seek(0L);
            return;
        }
        int target = current - 1;
        if (target < 0) {
            target = repeat == Repeat.ALL ? queue.size() - 1 : 0;
        }
        playIndex(target);
    }

    public void seek(long ms) {
        MixerTrack track = currentTrack();
        if (track == null) {
            return;
        }
        long max = track.durationMs() > 0L ? Math.max(0L, track.durationMs() - 1000L) : Long.MAX_VALUE / 4L;
        long target = Math.max(0L, Math.min(max, ms));
        if (playing) {
            holdAt(target);
        } else {
            pausedMs = target;
            revision++;
        }
        changed();
    }

    public void playIndex(int index) {
        if (index < 0 || index >= queue.size()) {
            return;
        }
        current = index;
        playing = true;
        atEnd = false;
        holdAt(0L);
        pickShuffleNext();
        changed();
    }

    /** Freezes the clock at {@code ms} until {@link #ready} or the timeout, and has clients (re)start their stream. */
    private void holdAt(long ms) {
        pausedMs = ms;
        waiting = true;
        waitingSince = now();
        revision++;
    }

    private void startClock() {
        waiting = false;
        startTick = now() - pausedMs / 50L;
        changed();
    }

    /** A client has buffered the song for {@code revision}: start the clock, for everyone at once. */
    public void ready(int revision) {
        if (playing && waiting && revision == this.revision) {
            startClock();
        }
    }

    private void pickShuffleNext() {
        shuffleNext = -1;
        if (shuffle && queue.size() > 1 && level != null) {
            do {
                shuffleNext = level.random.nextInt(queue.size());
            } while (shuffleNext == current);
        }
    }

    /** The song that plays after this one, or -1 if the queue stops; clients fetch it ahead of time. */
    public int upNext() {
        if (current < 0 || current >= queue.size()) {
            return -1;
        }
        if (repeat == Repeat.ONE) {
            return current;
        }
        if (shuffle && queue.size() > 1) {
            return shuffleNext >= 0 && shuffleNext < queue.size() && shuffleNext != current ? shuffleNext : -1;
        }
        return current + 1 < queue.size() ? current + 1 : repeat == Repeat.ALL ? 0 : -1;
    }

    public void remove(int index) {
        if (index < 0 || index >= queue.size()) {
            return;
        }
        queue.remove(index);
        if (queue.isEmpty()) {
            current = -1;
            playing = false;
            waiting = false;
            pausedMs = 0L;
        } else if (index < current) {
            current--;
        } else if (index == current) {
            current = Math.min(current, queue.size() - 1);
            if (playing) {
                playIndex(current);
                return;
            }
            pausedMs = 0L;
            revision++;
        }
        changed();
    }

    /** Swaps a song with its neighbour, {@code offset} being -1 (up) or 1 (down). */
    public void move(int index, int offset) {
        int target = index + Integer.signum(offset);
        if (index < 0 || index >= queue.size() || target < 0 || target >= queue.size()) {
            return;
        }
        Collections.swap(queue, index, target);
        if (current == index) {
            current = target;
        } else if (current == target) {
            current = index;
        }
        changed();
    }

    public void clear() {
        queue.clear();
        current = -1;
        playing = false;
        waiting = false;
        pausedMs = 0L;
        atEnd = false;
        changed();
    }

    public void setVolume(int volume) {
        int clamped = Math.max(0, Math.min(100, volume));
        if (clamped != this.volume) {
            this.volume = clamped;
            changed();
        }
    }

    public void cycleRepeat() {
        repeat = Repeat.values()[(repeat.ordinal() + 1) % Repeat.values().length];
        changed();
    }

    public void toggleShuffle() {
        shuffle = !shuffle;
        pickShuffleNext();
        changed();
    }

    /** Moves to the next song; {@code natural} is true when the current one simply ended. */
    private void advance(boolean natural) {
        if (queue.isEmpty()) {
            return;
        }
        if (natural && repeat == Repeat.ONE) {
            playIndex(current);
            return;
        }
        int target;
        if (shuffle && queue.size() > 1) {
            target = shuffleNext;
            while (target < 0 || target >= queue.size() || target == current) {
                target = level == null ? 0 : level.random.nextInt(queue.size());
            }
        } else {
            target = current + 1;
        }
        if (target >= queue.size()) {
            if (repeat == Repeat.ALL || !natural) {
                target = 0;
            } else {
                playing = false;
                waiting = false;
                pausedMs = 0L;
                atEnd = true;
                changed();
                return;
            }
        }
        playIndex(target);
    }

    private long now() {
        return level == null ? 0L : level.getGameTime();
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ---- read access, both sides ----

    /** How far into the current song the mixer is, by the level clock. */
    public long elapsedMs() {
        if (!playing || waiting) {
            return pausedMs;
        }
        return Math.max(0L, (now() - startTick) * 50L);
    }

    @Nullable
    public MixerTrack currentTrack() {
        return current >= 0 && current < queue.size() ? queue.get(current) : null;
    }

    public List<MixerTrack> queue() {
        return Collections.unmodifiableList(queue);
    }

    public List<BlockPos> speakers() {
        return Collections.unmodifiableList(speakers);
    }

    public int currentIndex() {
        return current;
    }

    public boolean isPlaying() {
        return playing;
    }

    public int volume() {
        return volume;
    }

    public Repeat repeat() {
        return repeat;
    }

    public boolean shuffle() {
        return shuffle;
    }

    /** The clock is held while a listener gets the song ready. */
    public boolean isWaiting() {
        return playing && waiting;
    }

    public int revision() {
        return revision;
    }

    // ---- saving and syncing ----

    @Override
    public void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (MixerTrack track : queue) {
            list.add(track.save());
        }
        tag.put("Queue", list);
        long[] speakerArray = new long[speakers.size()];
        for (int i = 0; i < speakers.size(); i++) {
            speakerArray[i] = speakers.get(i).asLong();
        }
        tag.putLongArray("Speakers", speakerArray);
        tag.putInt("Current", current);
        tag.putBoolean("Playing", playing);
        tag.putLong("StartTick", startTick);
        tag.putLong("PausedMs", pausedMs);
        tag.putInt("Volume", volume);
        tag.putInt("Repeat", repeat.ordinal());
        tag.putBoolean("Shuffle", shuffle);
        tag.putInt("Revision", revision);
        tag.putBoolean("AtEnd", atEnd);
        tag.putBoolean("Waiting", waiting);
        tag.putLong("WaitingSince", waitingSince);
        tag.putInt("ShuffleNext", shuffleNext);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        queue.clear();
        ListTag list = tag.getList("Queue", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && queue.size() < MAX_QUEUE; i++) {
            MixerTrack track = MixerTrack.load(list.getCompound(i));
            if (track != null) {
                queue.add(track);
            }
        }
        speakers.clear();
        for (long packed : tag.getLongArray("Speakers")) {
            if (speakers.size() < MAX_SPEAKERS) {
                speakers.add(BlockPos.of(packed));
            }
        }
        current = tag.contains("Current") ? tag.getInt("Current") : -1;
        playing = tag.getBoolean("Playing");
        startTick = tag.getLong("StartTick");
        pausedMs = tag.getLong("PausedMs");
        volume = tag.contains("Volume") ? Math.max(0, Math.min(100, tag.getInt("Volume"))) : 80;
        repeat = Repeat.byId(tag.getInt("Repeat"));
        shuffle = tag.getBoolean("Shuffle");
        revision = tag.getInt("Revision");
        atEnd = tag.getBoolean("AtEnd");
        waiting = tag.getBoolean("Waiting");
        waitingSince = tag.getLong("WaitingSince");
        shuffleNext = tag.contains("ShuffleNext") ? tag.getInt("ShuffleNext") : -1;
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MixerAudioClient.track(worldPosition));
        }
    }
}
