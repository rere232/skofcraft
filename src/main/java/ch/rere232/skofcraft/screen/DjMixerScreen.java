package ch.rere232.skofcraft.screen;

import ch.rere232.skofcraft.audio.MixerTrack;
import ch.rere232.skofcraft.blockentity.DjMixerBlockEntity;
import ch.rere232.skofcraft.client.audio.MediaTools;
import ch.rere232.skofcraft.client.audio.MixerAudioClient;
import ch.rere232.skofcraft.network.MixerActionC2SPacket;
import ch.rere232.skofcraft.network.MixerAddC2SPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The DJ mixer desk: search YouTube or YouTube Music (or paste a link) on the left, and run the show on the right
 * with the now-playing deck, transport buttons, volume and the queue. Everything except searching goes through the
 * server, so everyone at the mixer sees and hears the same thing.
 */
@OnlyIn(Dist.CLIENT)
public class DjMixerScreen extends Screen {
    private static final int W = 360;
    private static final int H = 236;
    private static final int COL_W = 168;
    private static final int RESULT_ROWS = 7;
    private static final int RESULT_H = 20;
    private static final int QUEUE_ROWS = 6;
    private static final int QUEUE_H = 12;
    private static final double MAX_DISTANCE_SQR = 64.0D;

    private static final int COLOR_PANEL = 0xF0141218;
    private static final int COLOR_BORDER = 0xFF8A5CF6;
    private static final int COLOR_INSET = 0xFF0C0B10;
    private static final int COLOR_ROW = 0xFF1C1A24;
    private static final int COLOR_HOVER = 0xFF2C2840;
    private static final int COLOR_PINK = 0xFFFF4FA3;
    private static final int COLOR_CYAN = 0xFF3DE0E0;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_DIM = 0xFF9A98A6;
    private static final int COLOR_WARN = 0xFFFFC857;
    private static final int COLOR_BAD = 0xFFFF5A5A;
    private static final int COLOR_GOOD = 0xFF6BE675;

    private final BlockPos pos;
    private int left;
    private int top;
    private int ticks;

    private EditBox search;
    private Button sourceButton;
    private Button addAllButton;
    private Button installButton;
    private Button updateButton;
    private Button playButton;
    private Button repeatButton;
    private Button shuffleButton;
    private Button clearButton;
    private VolumeSlider volumeSlider;

    private boolean music;
    private List<MixerTrack> results = new ArrayList<>();
    private Component resultsStatus = Component.empty();
    private boolean busy;
    private int resultsScroll;
    private int queueScroll;
    private int pendingVolume = -1;
    private int lastVolumeSend;

    public DjMixerScreen(BlockPos pos) {
        super(Component.translatable("block.skofcraft.dj_mixer"));
        this.pos = pos;
    }

    public BlockPos pos() {
        return pos;
    }

    private static net.minecraft.network.chat.MutableComponent tr(String key, Object... args) {
        return Component.translatable("screen.skofcraft.dj_mixer." + key, args);
    }

    @Nullable
    private DjMixerBlockEntity mixer() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getBlockEntity(pos) instanceof DjMixerBlockEntity mixer ? mixer : null;
    }

    @Override
    protected void init() {
        MediaTools.ensureChecked();
        left = (width - W) / 2;
        top = (height - H) / 2;
        int lx = left + 8;
        int rx = left + 184;

        String previous = search == null ? "" : search.getValue();
        search = new EditBox(font, lx + 1, top + 23, 118, 14, tr("search"));
        search.setMaxLength(300);
        search.setHint(tr("search_hint"));
        search.setValue(previous);
        addRenderableWidget(search);
        sourceButton = addRenderableWidget(Button.builder(sourceLabel(), b -> {
            music = !music;
            b.setMessage(sourceLabel());
        }).bounds(lx + 124, top + 22, 44, 16).tooltip(Tooltip.create(tr("source.tooltip"))).build());

        addAllButton = addRenderableWidget(Button.builder(tr("add_all"), b -> queue(results, false))
                .bounds(lx, top + 200, 60, 14).build());
        updateButton = addRenderableWidget(Button.builder(tr("update"), b -> MediaTools.updateYtDlp())
                .bounds(lx + 108, top + 200, 60, 14).tooltip(Tooltip.create(tr("update.tooltip"))).build());
        installButton = addRenderableWidget(Button.builder(tr("install"), b -> MediaTools.install())
                .bounds(lx + 24, top + 150, 120, 20).build());

        int bx = rx;
        addRenderableWidget(transport("⏮", "previous", MixerActionC2SPacket.PREVIOUS, bx));
        playButton = addRenderableWidget(transport("▶", "play", MixerActionC2SPacket.PLAY_PAUSE, bx + 28));
        addRenderableWidget(transport("⏹", "stop", MixerActionC2SPacket.STOP, bx + 56));
        addRenderableWidget(transport("⏭", "next", MixerActionC2SPacket.NEXT, bx + 84));
        repeatButton = addRenderableWidget(transport("↻", "repeat.off", MixerActionC2SPacket.REPEAT, bx + 116));
        shuffleButton = addRenderableWidget(transport("⇄", "shuffle.off", MixerActionC2SPacket.SHUFFLE, bx + 144));

        DjMixerBlockEntity mixer = mixer();
        volumeSlider = addRenderableWidget(new VolumeSlider(rx, top + 116, COL_W, 16, mixer == null ? 80 : mixer.volume()));
        clearButton = addRenderableWidget(Button.builder(tr("clear"), b -> send(MixerActionC2SPacket.CLEAR, 0))
                .bounds(rx + COL_W - 40, top + 136, 40, 12).build());

        setInitialFocus(search);
        refreshWidgets();
    }

    private Button transport(String icon, String tooltip, int action, int x) {
        return Button.builder(Component.literal(icon), b -> send(action, 0))
                .bounds(x, top + 94, 24, 18).tooltip(Tooltip.create(tr(tooltip))).build();
    }

    private Component sourceLabel() {
        return tr(music ? "source.music" : "source.youtube");
    }

    private void send(int action, long value) {
        SkofcraftNetwork.CHANNEL.sendToServer(new MixerActionC2SPacket(pos, action, value));
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    // ---- searching and queueing ----

    private void runSearch() {
        String text = search.getValue().trim();
        if (text.isEmpty() || busy) {
            return;
        }
        if (!MediaTools.ready()) {
            resultsStatus = tr("tools_missing.short").withStyle(s -> s.withColor(COLOR_WARN));
            return;
        }
        busy = true;
        resultsScroll = 0;
        boolean link = MediaTools.isYouTubeUrl(text);
        resultsStatus = tr(link ? "reading_link" : "searching");
        CompletableFuture<List<MixerTrack>> future = link ? MediaTools.resolveLink(text) : MediaTools.search(text, music);
        future.whenComplete((tracks, error) -> Minecraft.getInstance().execute(() -> {
            busy = false;
            if (error != null) {
                Throwable cause = error.getCause() != null ? error.getCause() : error;
                resultsStatus = Component.literal(String.valueOf(cause.getMessage())).withStyle(s -> s.withColor(COLOR_BAD));
                return;
            }
            results = tracks;
            resultsStatus = tracks.isEmpty() ? tr("no_results") : tr("results", tracks.size());
            // A pasted link to a single song goes straight in the queue
            if (link && tracks.size() == 1) {
                queue(tracks, false);
            }
        }));
    }

    private void queue(List<MixerTrack> tracks, boolean playNow) {
        if (tracks.isEmpty() || busy) {
            return;
        }
        List<MixerTrack> batch = new ArrayList<>(tracks.subList(0, Math.min(tracks.size(), MixerAddC2SPacket.MAX_TRACKS)));
        if (batch.stream().allMatch(track -> track.durationMs() > 0L)) {
            SkofcraftNetwork.CHANNEL.sendToServer(new MixerAddC2SPacket(pos, batch, playNow));
            resultsStatus = tr(playNow ? "playing" : "queued", batch.size());
            return;
        }
        busy = true;
        resultsStatus = tr("resolving");
        MediaTools.withDurations(batch).whenComplete((done, error) -> Minecraft.getInstance().execute(() -> {
            busy = false;
            if (error != null || done.isEmpty()) {
                resultsStatus = tr("resolve_failed").withStyle(s -> s.withColor(COLOR_BAD));
                return;
            }
            SkofcraftNetwork.CHANNEL.sendToServer(new MixerAddC2SPacket(pos, done, playNow));
            resultsStatus = tr(playNow ? "playing" : "queued", done.size());
        }));
    }

    // ---- ticking ----

    @Override
    public void tick() {
        ticks++;
        search.tick();
        Minecraft mc = Minecraft.getInstance();
        DjMixerBlockEntity mixer = mixer();
        if (mixer == null || mc.player == null || mc.player.distanceToSqr(pos.getCenter()) > MAX_DISTANCE_SQR) {
            onClose();
            return;
        }
        if (pendingVolume >= 0 && ticks - lastVolumeSend >= 4) {
            send(MixerActionC2SPacket.VOLUME, pendingVolume);
            pendingVolume = -1;
            lastVolumeSend = ticks;
        }
        refreshWidgets();
    }

    private void refreshWidgets() {
        DjMixerBlockEntity mixer = mixer();
        boolean ready = MediaTools.ready();
        MediaTools.State state = MediaTools.state();
        boolean canInstall = state == MediaTools.State.MISSING || state == MediaTools.State.FAILED;
        installButton.visible = !ready && canInstall;
        installButton.active = installButton.visible;
        updateButton.visible = MediaTools.hasYtDlp();
        updateButton.active = ready;
        addAllButton.visible = ready && results.size() > 1;
        addAllButton.active = !busy;
        search.setEditable(ready || state == MediaTools.State.CHECKING);
        if (mixer == null) {
            return;
        }
        playButton.setMessage(Component.literal(mixer.isPlaying() ? "⏸" : "▶"));
        playButton.setTooltip(Tooltip.create(tr(mixer.isPlaying() ? "pause" : "play")));
        DjMixerBlockEntity.Repeat repeat = mixer.repeat();
        repeatButton.setMessage(Component.literal(repeat == DjMixerBlockEntity.Repeat.ONE ? "↻1" : "↻")
                .withStyle(s -> s.withColor(repeat == DjMixerBlockEntity.Repeat.OFF ? COLOR_DIM : COLOR_GOOD)));
        repeatButton.setTooltip(Tooltip.create(tr("repeat." + repeat.name().toLowerCase(java.util.Locale.ROOT))));
        shuffleButton.setMessage(Component.literal("⇄").withStyle(s -> s.withColor(mixer.shuffle() ? COLOR_GOOD : COLOR_DIM)));
        shuffleButton.setTooltip(Tooltip.create(tr(mixer.shuffle() ? "shuffle.on" : "shuffle.off")));
        clearButton.active = !mixer.queue().isEmpty();
        queueScroll = Mth.clamp(queueScroll, 0, Math.max(0, mixer.queue().size() - QUEUE_ROWS));
    }

    // ---- input ----

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (search.isFocused() && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
            runSearch();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0) {
            return false;
        }
        DjMixerBlockEntity mixer = mixer();
        if (mixer == null) {
            return false;
        }
        if (clickResults(mouseX, mouseY) || clickQueue(mixer, mouseX, mouseY) || clickSeek(mixer, mouseX, mouseY)) {
            click();
            return true;
        }
        return false;
    }

    private boolean clickResults(double mouseX, double mouseY) {
        if (!MediaTools.ready()) {
            return false;
        }
        int row = resultRowAt(mouseX, mouseY);
        if (row < 0) {
            return false;
        }
        MixerTrack track = results.get(row);
        int x = left + 8;
        boolean playNow = mouseX >= x + COL_W - 30 && mouseX < x + COL_W - 16;
        queue(List.of(track), playNow);
        return true;
    }

    private boolean clickQueue(DjMixerBlockEntity mixer, double mouseX, double mouseY) {
        int row = queueRowAt(mixer, mouseX, mouseY);
        if (row < 0) {
            return false;
        }
        int x = left + 184 + COL_W;
        if (mouseX >= x - 12) {
            send(MixerActionC2SPacket.REMOVE, row);
        } else if (mouseX >= x - 24) {
            send(MixerActionC2SPacket.MOVE_DOWN, row);
        } else if (mouseX >= x - 36) {
            send(MixerActionC2SPacket.MOVE_UP, row);
        } else {
            send(MixerActionC2SPacket.PLAY_INDEX, row);
        }
        return true;
    }

    private boolean clickSeek(DjMixerBlockEntity mixer, double mouseX, double mouseY) {
        MixerTrack track = mixer.currentTrack();
        int x = left + 190;
        int y = top + 74;
        int w = COL_W - 12;
        if (track == null || track.durationMs() <= 0L || mouseX < x || mouseX > x + w || mouseY < y - 3 || mouseY > y + 6) {
            return false;
        }
        send(MixerActionC2SPacket.SEEK, (long) ((mouseX - x) / w * track.durationMs()));
        return true;
    }

    private int resultRowAt(double mouseX, double mouseY) {
        int x = left + 8;
        int y = top + 44;
        if (mouseX < x || mouseX >= x + COL_W || mouseY < y || mouseY >= y + RESULT_ROWS * RESULT_H) {
            return -1;
        }
        int row = resultsScroll + (int) ((mouseY - y) / RESULT_H);
        return row < results.size() ? row : -1;
    }

    private int queueRowAt(DjMixerBlockEntity mixer, double mouseX, double mouseY) {
        int x = left + 184;
        int y = top + 152;
        if (mouseX < x || mouseX >= x + COL_W || mouseY < y || mouseY >= y + QUEUE_ROWS * QUEUE_H) {
            return -1;
        }
        int row = queueScroll + (int) ((mouseY - y) / QUEUE_H);
        return row < mixer.queue().size() ? row : -1;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int step = delta > 0 ? -1 : 1;
        if (mouseX < left + 180) {
            resultsScroll = Mth.clamp(resultsScroll + step, 0, Math.max(0, results.size() - RESULT_ROWS));
        } else {
            DjMixerBlockEntity mixer = mixer();
            int size = mixer == null ? 0 : mixer.queue().size();
            queueScroll = Mth.clamp(queueScroll + step, 0, Math.max(0, size - QUEUE_ROWS));
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- drawing ----

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, COLOR_BORDER);
        g.fill(left, top, left + W, top + H, COLOR_PANEL);
        g.fillGradient(left, top, left + W, top + 18, 0xFF2A1440, 0xFF141218);
        g.drawString(font, Component.literal("SKOF").withStyle(s -> s.withColor(COLOR_PINK).withBold(true))
                .append(Component.literal(" DJ MIXER").withStyle(s -> s.withColor(COLOR_CYAN).withBold(true))), left + 8, top + 6, COLOR_TEXT);

        DjMixerBlockEntity mixer = mixer();
        if (mixer != null) {
            drawSpeakers(g, mixer);
        }
        drawResults(g, mouseX, mouseY);
        if (mixer != null) {
            drawNowPlaying(g, mixer, partialTick);
            drawQueue(g, mixer, mouseX, mouseY);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawSpeakers(GuiGraphics g, DjMixerBlockEntity mixer) {
        int count = mixer.speakers().size();
        Component label = count == 0 ? tr("no_speakers") : count == 1 ? tr("speaker_one") : tr("speakers", count);
        int color = count == 0 ? COLOR_BAD : COLOR_GOOD;
        g.drawString(font, label, left + W - 8 - font.width(label), top + 6, color);
    }

    private void drawResults(GuiGraphics g, int mouseX, int mouseY) {
        int x = left + 8;
        int y = top + 44;
        int h = RESULT_ROWS * RESULT_H;
        g.fill(x, y, x + COL_W, y + h, COLOR_INSET);

        if (!MediaTools.ready()) {
            drawToolsPanel(g, x, y);
        } else if (results.isEmpty()) {
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(tr("empty_results"), COL_W - 16);
            for (int i = 0; i < lines.size(); i++) {
                g.drawCenteredString(font, lines.get(i), x + COL_W / 2, y + h / 2 - lines.size() * 5 + i * 10, COLOR_DIM);
            }
        } else {
            int hovered = resultRowAt(mouseX, mouseY);
            for (int i = 0; i < RESULT_ROWS && resultsScroll + i < results.size(); i++) {
                int index = resultsScroll + i;
                MixerTrack track = results.get(index);
                int ry = y + i * RESULT_H;
                g.fill(x + 1, ry + 1, x + COL_W - 1, ry + RESULT_H - 1, index == hovered ? COLOR_HOVER : COLOR_ROW);
                int textW = COL_W - (index == hovered ? 36 : 8);
                g.drawString(font, fit(track.title(), textW), x + 4, ry + 2, COLOR_TEXT, false);
                String sub = track.artist() + (track.durationMs() > 0L ? "  " + MixerTrack.formatTime(track.durationMs()) : "");
                g.drawString(font, fit(sub, textW), x + 4, ry + 11, COLOR_DIM, false);
                if (index == hovered) {
                    drawIcon(g, x + COL_W - 30, ry + 4, "▶", mouseX, mouseY, COLOR_PINK);
                    drawIcon(g, x + COL_W - 15, ry + 4, "+", mouseX, mouseY, COLOR_CYAN);
                }
            }
            drawScrollbar(g, x + COL_W - 2, y, h, resultsScroll, RESULT_ROWS, results.size());
        }

        g.drawString(font, fit(resultsStatus.getString(), COL_W), x, top + 189, resultsStatus.getStyle().getColor() != null
                ? resultsStatus.getStyle().getColor().getValue() : COLOR_DIM, false);
        String tools = MediaTools.message();
        g.drawString(font, fit(tools, COL_W), x, top + 222, MediaTools.ready() ? 0xFF6A6878 : COLOR_WARN, false);
    }

    private void drawToolsPanel(GuiGraphics g, int x, int y) {
        MediaTools.State state = MediaTools.state();
        Component text;
        if (state == MediaTools.State.CHECKING || state == MediaTools.State.UNKNOWN) {
            text = tr("tools_checking");
        } else if (state == MediaTools.State.INSTALLING) {
            text = tr("tools_installing");
        } else {
            text = tr("tools_missing");
        }
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(text, COL_W - 12);
        for (int i = 0; i < lines.size() && i < 9; i++) {
            g.drawString(font, lines.get(i), x + 6, y + 6 + i * 10, COLOR_TEXT, false);
        }
        if (state == MediaTools.State.INSTALLING) {
            int barY = y + 110;
            g.fill(x + 10, barY, x + COL_W - 10, barY + 6, 0xFF2A2833);
            g.fill(x + 10, barY, x + 10 + (int) ((COL_W - 20) * Mth.clamp(MediaTools.progress(), 0.0D, 1.0D)), barY + 6, COLOR_CYAN);
        }
    }

    private void drawNowPlaying(GuiGraphics g, DjMixerBlockEntity mixer, float partialTick) {
        int x = left + 184;
        int y = top + 22;
        g.fill(x, y, x + COL_W, y + 68, COLOR_INSET);
        g.drawString(font, tr("now_playing"), x + 6, y + 4, COLOR_PINK, false);

        MixerTrack track = mixer.currentTrack();
        if (track == null) {
            g.drawString(font, tr("nothing"), x + 6, y + 18, COLOR_DIM, false);
            return;
        }
        drawMarquee(g, track.title(), x + 6, y + 16, COL_W - 12, partialTick);
        String status = MixerAudioClient.status(pos);
        if (mixer.isPlaying() && mixer.speakers().isEmpty()) {
            g.drawString(font, fit(tr("no_speakers_short").getString(), COL_W - 12), x + 6, y + 28, COLOR_BAD, false);
        } else if (status != null && !status.isEmpty()) {
            g.drawString(font, fit(status, COL_W - 12), x + 6, y + 28, status.startsWith("Error") ? COLOR_BAD : COLOR_WARN, false);
        } else {
            g.drawString(font, fit(track.artist(), COL_W - 12), x + 6, y + 28, COLOR_DIM, false);
        }
        drawEqualizer(g, x + 6, y + 40, COL_W - 12, 10, mixer.isPlaying() && status != null && status.isEmpty(), partialTick);

        long elapsed = Math.min(mixer.elapsedMs(), track.durationMs() > 0L ? track.durationMs() : Long.MAX_VALUE);
        int barX = x + 6;
        int barY = y + 52;
        int barW = COL_W - 12;
        g.fill(barX, barY, barX + barW, barY + 3, 0xFF2A2833);
        if (track.durationMs() > 0L) {
            int filled = (int) (barW * Mth.clamp((double) elapsed / track.durationMs(), 0.0D, 1.0D));
            g.fillGradient(barX, barY, barX + filled, barY + 3, COLOR_PINK, COLOR_CYAN);
            g.fill(barX + filled - 1, barY - 1, barX + filled + 1, barY + 4, COLOR_TEXT);
        }
        g.drawString(font, MixerTrack.formatTime(elapsed), barX, y + 58, COLOR_DIM, false);
        String total = track.durationMs() > 0L ? MixerTrack.formatTime(track.durationMs()) : "--:--";
        g.drawString(font, total, barX + barW - font.width(total), y + 58, COLOR_DIM, false);
    }

    private void drawMarquee(GuiGraphics g, String text, int x, int y, int w, float partialTick) {
        int textW = font.width(text);
        if (textW <= w) {
            g.drawString(font, text, x, y, COLOR_TEXT, false);
            return;
        }
        int gap = 40;
        int offset = (int) (((ticks + partialTick) * 0.75F) % (textW + gap));
        g.enableScissor(x, y - 1, x + w, y + 10);
        g.drawString(font, text, x - offset, y, COLOR_TEXT, false);
        g.drawString(font, text, x - offset + textW + gap, y, COLOR_TEXT, false);
        g.disableScissor();
    }

    /** Not a real spectrum, the samples never leave the sound thread; it just dances while the music is on. */
    private void drawEqualizer(GuiGraphics g, int x, int y, int w, int h, boolean active, float partialTick) {
        int bars = 26;
        int barW = w / bars;
        float t = (ticks + partialTick) * 0.25F;
        for (int i = 0; i < bars; i++) {
            float level = active
                    ? 0.15F + 0.85F * Math.abs(Mth.sin(t * (0.6F + (i * 37 % 11) * 0.09F) + i * 1.7F) * Mth.cos(t * 0.31F + i * 0.5F))
                    : 0.08F;
            int barH = Math.max(1, (int) (h * level));
            int color = lerpColor(COLOR_PINK, COLOR_CYAN, (float) i / (bars - 1));
            g.fill(x + i * barW, y + h - barH, x + i * barW + barW - 1, y + h, color);
        }
    }

    private void drawQueue(GuiGraphics g, DjMixerBlockEntity mixer, int mouseX, int mouseY) {
        int x = left + 184;
        List<MixerTrack> queue = mixer.queue();
        g.drawString(font, tr("queue", queue.size()), x, top + 138, COLOR_PINK, false);
        int y = top + 152;
        int h = QUEUE_ROWS * QUEUE_H;
        g.fill(x, y, x + COL_W, y + h, COLOR_INSET);
        if (queue.isEmpty()) {
            g.drawCenteredString(font, tr("queue_empty"), x + COL_W / 2, y + h / 2 - 4, COLOR_DIM);
            return;
        }
        int hovered = queueRowAt(mixer, mouseX, mouseY);
        for (int i = 0; i < QUEUE_ROWS && queueScroll + i < queue.size(); i++) {
            int index = queueScroll + i;
            MixerTrack track = queue.get(index);
            int ry = y + i * QUEUE_H;
            boolean current = index == mixer.currentIndex();
            if (index == hovered || current) {
                g.fill(x + 1, ry, x + COL_W - 1, ry + QUEUE_H, index == hovered ? COLOR_HOVER : 0xFF231B33);
            }
            String marker = current ? (mixer.isPlaying() ? "▶" : "❚❚") : Integer.toString(index + 1);
            g.drawString(font, marker, x + 3, ry + 2, current ? COLOR_PINK : COLOR_DIM, false);
            int textX = x + 18;
            if (index == hovered) {
                g.drawString(font, fit(track.title(), COL_W - 18 - 40), textX, ry + 2, COLOR_TEXT, false);
                int ix = x + COL_W;
                drawIcon(g, ix - 36, ry + 1, "▲", mouseX, mouseY, COLOR_TEXT);
                drawIcon(g, ix - 24, ry + 1, "▼", mouseX, mouseY, COLOR_TEXT);
                drawIcon(g, ix - 12, ry + 1, "✕", mouseX, mouseY, COLOR_BAD);
            } else {
                String time = track.durationMs() > 0L ? MixerTrack.formatTime(track.durationMs()) : "";
                g.drawString(font, fit(track.title(), COL_W - 18 - font.width(time) - 8), textX, ry + 2, current ? COLOR_TEXT : 0xFFD8D6E0, false);
                g.drawString(font, time, x + COL_W - 4 - font.width(time), ry + 2, COLOR_DIM, false);
            }
        }
        drawScrollbar(g, x + COL_W - 2, y, h, queueScroll, QUEUE_ROWS, queue.size());
    }

    private void drawIcon(GuiGraphics g, int x, int y, String icon, int mouseX, int mouseY, int color) {
        boolean hover = mouseX >= x && mouseX < x + 11 && mouseY >= y && mouseY < y + 11;
        g.fill(x, y, x + 11, y + 10, hover ? 0xFF4A4466 : 0xFF34304A);
        g.drawCenteredString(font, icon, x + 6, y + 1, color);
    }

    private void drawScrollbar(GuiGraphics g, int x, int y, int h, int scroll, int visible, int total) {
        if (total <= visible) {
            return;
        }
        int thumb = Math.max(8, h * visible / total);
        int thumbY = y + (h - thumb) * scroll / Math.max(1, total - visible);
        g.fill(x, y, x + 2, y + h, 0xFF2A2833);
        g.fill(x, thumbY, x + 2, thumbY + thumb, COLOR_BORDER);
    }

    private String fit(String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width("…"))) + "…";
    }

    private static int lerpColor(int from, int to, float t) {
        int r = (int) Mth.lerp(t, (from >> 16) & 0xFF, (to >> 16) & 0xFF);
        int gr = (int) Mth.lerp(t, (from >> 8) & 0xFF, (to >> 8) & 0xFF);
        int b = (int) Mth.lerp(t, from & 0xFF, to & 0xFF);
        return 0xFF000000 | r << 16 | gr << 8 | b;
    }

    private final class VolumeSlider extends AbstractSliderButton {
        VolumeSlider(int x, int y, int width, int height, int volume) {
            super(x, y, width, height, Component.empty(), volume / 100.0D);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(tr("volume", (int) Math.round(value * 100.0D)));
        }

        @Override
        protected void applyValue() {
            pendingVolume = (int) Math.round(value * 100.0D);
        }
    }
}
