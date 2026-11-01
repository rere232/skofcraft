package ch.rere232.skofcraft.screen;

import ch.rere232.skofcraft.network.RadarContactsS2CPacket;
import ch.rere232.skofcraft.network.RadarScanC2SPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import ch.rere232.skofcraft.radar.RadarCategory;
import ch.rere232.skofcraft.radar.RadarContact;
import ch.rere232.skofcraft.radar.RadarMode;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Radar scope: a sweeping PPI display, north up, with the radar at the centre. The server sends a fresh sweep
 * every {@link #REQUEST_INTERVAL} ticks; blips glow when the beam passes and fade until it comes round again.
 */
@OnlyIn(Dist.CLIENT)
public class RadarScreen extends Screen {
    private static final int PANEL_W = 300;
    private static final int PANEL_H = 198;
    private static final int R = 80;
    private static final int SIDE_W = 106;
    private static final int LIST_ROWS = 8;
    private static final int ROW_H = 11;
    private static final int REQUEST_INTERVAL = 10;
    private static final long SWEEP_MS = 4000L;
    private static final int TRAIL_LINES = 28;
    private static final float TRAIL_STEP = (float) Math.toRadians(1.6D);
    private static final int HOVER_RADIUS = 4;

    private static final int COLOR_SCOPE = 0xFF07160C;
    private static final int COLOR_RING = 0xFF1C4A2A;
    private static final int COLOR_RIM = 0xFF3DDC97;
    private static final int TRAIL_RGB = 0x40FF70;
    private static final int COLOR_WARN = 0xFFE07040;

    private final BlockPos pos;
    private List<RadarContact> contacts = new ArrayList<>();
    private RadarMode mode = RadarMode.ALL;
    private int range = 64;
    private boolean powered;
    private int energy;
    private int signal;
    private boolean received;
    private int ticks;
    private float lastSweep = -1.0F;
    private Button modeButton;
    private int leftPos;
    private int topPos;

    public RadarScreen(BlockPos pos) {
        super(Component.translatable("block.skofcraft.radar"));
        this.pos = pos;
    }

    public BlockPos pos() {
        return pos;
    }

    public void update(RadarContactsS2CPacket packet) {
        contacts = packet.contacts;
        mode = packet.mode;
        range = Math.max(1, packet.range);
        powered = packet.powered;
        energy = packet.energy;
        signal = packet.signal;
        received = true;
        if (modeButton != null) {
            modeButton.setMessage(modeLabel());
        }
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (this.width - PANEL_W) / 2;
        topPos = (this.height - PANEL_H) / 2;
        modeButton = addRenderableWidget(Button.builder(modeLabel(), button -> {
            mode = mode.next();
            button.setMessage(modeLabel());
            SkofcraftNetwork.CHANNEL.sendToServer(new RadarScanC2SPacket(pos, mode.ordinal()));
        }).bounds(sideX(), topPos + 58, SIDE_W, 18).build());
        SkofcraftNetwork.CHANNEL.sendToServer(new RadarScanC2SPacket(pos, -1));
    }

    private Component modeLabel() {
        return Component.translatable("screen.skofcraft.radar.mode", Component.translatable(mode.key()));
    }

    @Override
    public void tick() {
        super.tick();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.distanceToSqr(pos.getCenter()) > 64.0D) {
            onClose();
            return;
        }
        if (++ticks % REQUEST_INTERVAL == 0) {
            SkofcraftNetwork.CHANNEL.sendToServer(new RadarScanC2SPacket(pos, -1));
        }
    }

    private int centerX() {
        return leftPos + 12 + R;
    }

    private int centerY() {
        return topPos + MachineScreenHelper.HEADER_HEIGHT + 10 + R;
    }

    private int sideX() {
        return leftPos + PANEL_W - 8 - SIDE_W;
    }

    private int blipX(RadarContact contact) {
        return centerX() + Math.round(contact.dx() * R / range);
    }

    private int blipY(RadarContact contact) {
        return centerY() + Math.round(contact.dz() * R / range);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        MachineScreenHelper.drawPanel(g, leftPos, topPos, PANEL_W, PANEL_H);
        g.drawString(this.font, this.title, leftPos + 8, topPos + 5, MachineScreenHelper.COLOR_TEXT, false);

        float sweep = (float) ((Util.getMillis() % SWEEP_MS) / (double) SWEEP_MS * Math.PI * 2.0D);
        RadarContact hovered = hoveredContact(mouseX, mouseY);
        drawScope(g, sweep);
        drawBlips(g, sweep, hovered);
        pingCrossings(sweep);
        drawSidePanel(g, hovered);

        super.render(g, mouseX, mouseY, partialTick);

        if (hovered != null) {
            g.renderComponentTooltip(this.font, tooltip(hovered), mouseX, mouseY);
        }
    }

    private void drawScope(GuiGraphics g, float sweep) {
        int cx = centerX();
        int cy = centerY();
        MachineScreenHelper.fillRounded(g, cx - R - 3, cy - R - 3, cx + R + 4, cy + R + 4, MachineScreenHelper.COLOR_WELL);
        for (int dy = -R; dy <= R; dy++) {
            int w = (int) Math.sqrt(R * R - dy * dy);
            g.fill(cx - w, cy + dy, cx + w + 1, cy + dy + 1, COLOR_SCOPE);
        }
        g.fill(cx - R, cy, cx + R + 1, cy + 1, COLOR_RING);
        g.fill(cx, cy - R, cx + 1, cy + R + 1, COLOR_RING);
        for (int ring = 1; ring <= 4; ring++) {
            circle(g, cx, cy, R * ring / 4, ring == 4 ? COLOR_RIM : COLOR_RING);
        }

        // Fading afterglow behind the beam, then the beam itself
        for (int i = TRAIL_LINES; i > 0; i--) {
            int alpha = (int) (0x70 * (1.0F - i / (float) TRAIL_LINES));
            ray(g, cx, cy, sweep - i * TRAIL_STEP, (alpha << 24) | TRAIL_RGB);
        }
        ray(g, cx, cy, sweep, 0xFF000000 | TRAIL_RGB);

        g.drawCenteredString(this.font, "N", cx, cy - R - 12, MachineScreenHelper.COLOR_TEXT_DIM);
        g.drawCenteredString(this.font, "S", cx, cy + R + 5, MachineScreenHelper.COLOR_TEXT_DIM);
        g.drawString(this.font, "E", cx + R + 5, cy - 4, MachineScreenHelper.COLOR_TEXT_DIM, false);
        g.drawString(this.font, "W", cx - R - 10, cy - 4, MachineScreenHelper.COLOR_TEXT_DIM, false);
        // The radar itself
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, MachineScreenHelper.COLOR_TEXT);
    }

    private static void circle(GuiGraphics g, int cx, int cy, int radius, int color) {
        int steps = Math.max(24, (int) (radius * Math.PI * 2.0D));
        for (int i = 0; i < steps; i++) {
            double angle = i * Math.PI * 2.0D / steps;
            int x = cx + (int) Math.round(Math.sin(angle) * radius);
            int y = cy - (int) Math.round(Math.cos(angle) * radius);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static void ray(GuiGraphics g, int cx, int cy, float angle, int color) {
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        for (int d = 2; d <= R; d++) {
            int x = cx + (int) Math.round(sin * d);
            int y = cy - (int) Math.round(cos * d);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    /** How long ago (as a fraction of a turn) the beam passed this bearing. */
    private static float sinceBeam(float sweep, RadarContact contact) {
        double phase = sweep - Math.toRadians(contact.bearing());
        phase = phase - Math.floor(phase / (Math.PI * 2.0D)) * Math.PI * 2.0D;
        return (float) (phase / (Math.PI * 2.0D));
    }

    private void drawBlips(GuiGraphics g, float sweep, @Nullable RadarContact hovered) {
        for (RadarContact contact : contacts) {
            int x = blipX(contact);
            int y = blipY(contact);
            float glow = 1.0F - sinceBeam(sweep, contact) * 0.75F;
            int color = ((int) (glow * 255.0F) << 24) | (contact.category().color & 0xFFFFFF);
            int size = contact.category() == RadarCategory.AIRCRAFT || contact.category() == RadarCategory.SHIP ? 2 : 1;
            g.fill(x - size, y - size, x + size + 1, y + size + 1, color);
            // Altitude cue: a tick above for contacts well above the radar, below for those under it
            if (contact.dy() > 8.0F) {
                g.fill(x, y - size - 2, x + 1, y - size - 1, color);
            } else if (contact.dy() < -8.0F) {
                g.fill(x, y + size + 1, x + 1, y + size + 2, color);
            }
            if (contact == hovered) {
                g.renderOutline(x - size - 2, y - size - 2, size * 2 + 5, size * 2 + 5, MachineScreenHelper.COLOR_TEXT);
            }
        }
    }

    /** A soft ping when the beam sweeps over anything but wildlife. */
    private void pingCrossings(float sweep) {
        float previous = lastSweep;
        lastSweep = sweep;
        if (previous < 0.0F) {
            return;
        }
        for (RadarContact contact : contacts) {
            if (contact.category() == RadarCategory.CREATURE) {
                continue;
            }
            float bearing = (float) Math.toRadians(contact.bearing());
            boolean crossed = previous <= sweep ? bearing > previous && bearing <= sweep : bearing > previous || bearing <= sweep;
            if (crossed) {
                float pitch = contact.category() == RadarCategory.AIRCRAFT ? 1.8F : 1.4F;
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BIT.value(), pitch, 0.2F));
                return;
            }
        }
    }

    private void drawSidePanel(GuiGraphics g, @Nullable RadarContact hovered) {
        int x = sideX();
        int y = topPos + MachineScreenHelper.HEADER_HEIGHT + 6;
        if (!received) {
            g.drawString(this.font, Component.translatable("screen.skofcraft.radar.connecting"), x, y, MachineScreenHelper.COLOR_TEXT_DIM, false);
            return;
        }
        g.drawString(this.font, Component.translatable("screen.skofcraft.radar.range", range), x, y, MachineScreenHelper.COLOR_TEXT, false);
        g.drawString(this.font, powered
                        ? Component.translatable("screen.skofcraft.radar.powered", energy)
                        : Component.translatable("screen.skofcraft.radar.unpowered"),
                x, y + 11, powered ? MachineScreenHelper.COLOR_PROGRESS : COLOR_WARN, false);
        g.drawString(this.font, Component.translatable("screen.skofcraft.radar.signal", signal), x, y + 22,
                signal > 0 ? 0xFFFF5050 : MachineScreenHelper.COLOR_TEXT_DIM, false);

        int listTop = topPos + 82;
        g.drawString(this.font, Component.translatable("screen.skofcraft.radar.contacts", contacts.size()), x, listTop, MachineScreenHelper.COLOR_ACCENT, false);
        MachineScreenHelper.drawSeparator(g, x, listTop + 10, SIDE_W);
        if (contacts.isEmpty()) {
            g.drawString(this.font, Component.translatable("screen.skofcraft.radar.clear"), x, listTop + 15, MachineScreenHelper.COLOR_TEXT_DIM, false);
            return;
        }
        for (int i = 0; i < Math.min(LIST_ROWS, contacts.size()); i++) {
            RadarContact contact = contacts.get(i);
            int rowY = listTop + 15 + i * ROW_H;
            if (contact == hovered) {
                g.fill(x - 2, rowY - 2, x + SIDE_W, rowY + ROW_H - 2, MachineScreenHelper.COLOR_HIGHLIGHT);
            }
            g.fill(x, rowY + 1, x + 5, rowY + 6, contact.category().color);
            String distance = Math.round(contact.horizontalDistance()) + "m";
            int distanceW = this.font.width(distance);
            String name = this.font.plainSubstrByWidth(contact.name().getString(), SIDE_W - 10 - distanceW - 4);
            g.drawString(this.font, name, x + 8, rowY, MachineScreenHelper.COLOR_TEXT, false);
            g.drawString(this.font, distance, x + SIDE_W - distanceW, rowY, MachineScreenHelper.COLOR_TEXT_DIM, false);
        }
        if (contacts.size() > LIST_ROWS) {
            g.drawString(this.font, Component.translatable("screen.skofcraft.radar.more", contacts.size() - LIST_ROWS),
                    x, listTop + 15 + LIST_ROWS * ROW_H, MachineScreenHelper.COLOR_TEXT_DIM, false);
        }
    }

    @Nullable
    private RadarContact hoveredContact(int mouseX, int mouseY) {
        int x = sideX();
        int listTop = topPos + 82 + 15;
        if (mouseX >= x - 2 && mouseX < x + SIDE_W && mouseY >= listTop - 2) {
            int row = (mouseY - listTop + 2) / ROW_H;
            if (row < Math.min(LIST_ROWS, contacts.size())) {
                return contacts.get(row);
            }
        }
        RadarContact best = null;
        int bestDistance = HOVER_RADIUS * HOVER_RADIUS + 1;
        for (RadarContact contact : contacts) {
            int dx = blipX(contact) - mouseX;
            int dy = blipY(contact) - mouseY;
            int distance = dx * dx + dy * dy;
            if (distance < bestDistance) {
                best = contact;
                bestDistance = distance;
            }
        }
        return best;
    }

    private List<Component> tooltip(RadarContact contact) {
        List<Component> lines = new ArrayList<>();
        lines.add(contact.name().copy().withStyle(style -> style.withColor(contact.category().color & 0xFFFFFF)));
        lines.add(Component.translatable(contact.category().key()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("screen.skofcraft.radar.tooltip.distance", Math.round(contact.horizontalDistance()),
                Math.round(contact.bearing()) % 360).withStyle(ChatFormatting.GRAY));
        int dy = Math.round(contact.dy());
        lines.add(Component.translatable("screen.skofcraft.radar.tooltip.altitude", Math.round(pos.getY() + 0.5F + contact.dy()), (dy >= 0 ? "+" : "") + dy)
                .withStyle(ChatFormatting.GRAY));
        return lines;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
