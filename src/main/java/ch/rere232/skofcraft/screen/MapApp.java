package ch.rere232.skofcraft.screen;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.AircraftCarrier;
import ch.rere232.skofcraft.entity.OrbitalRod;
import ch.rere232.skofcraft.entity.PilotedAircraft;
import ch.rere232.skofcraft.network.SatelliteLinkC2SPacket;
import ch.rere232.skofcraft.network.SatelliteLinkS2CPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

/**
 * Sat Map: live top-down imagery of the terrain the phone's satellites can see (whatever the game has loaded around
 * you), with players and aircraft on it. Needs a recon satellite in orbit over this dimension. With strike satellites,
 * arm STRIKE and tap the map to drop a tungsten rod there. Drag to pan, scroll or +/- to zoom, the dot to recenter.
 */
@OnlyIn(Dist.CLIENT)
public class MapApp implements PhoneApp {
    private static final ResourceLocation TEXTURE_ID = ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "phone_map");
    private static final int[] SCALES = {1, 2, 4, 8};
    private static final int TOOLBAR_H = 14;
    private static final int INFO_H = 10;
    private static final int STATUS_INTERVAL = 40;
    private static final long MESSAGE_MS = 4000L;
    private static final int MARKER_TICKS = OrbitalRod.WARNING_TICKS + 40;
    private static final int UNLOADED = 0xFF14100E;
    private static final int COLOR_STRIKE = 0xFFE5484D;

    // Latest status from the server, shared by every opening of the app
    private static boolean statusKnown;
    private static int recon;
    private static int strikeTotal;
    private static int strikeReady;
    private static int secondsToNext;
    @Nullable
    private static Component message;
    private static long messageAt;
    private static long markerUntil;
    private static int markerX;
    private static int markerZ;
    /** The Giga ICBM target this player marked on the map, drawn as a purple cross. */
    private static boolean icbmMarked;
    private static int icbmX;
    private static int icbmZ;

    private final Font font;
    private DynamicTexture texture;
    private int texW;
    private int texH;
    private int scaleIndex = 1;
    private boolean follow = true;
    private double centerX;
    private double centerZ;
    private boolean dirty = true;
    private boolean armed;
    private int ticks;
    // Where the app was last drawn, for mouse handling
    private int areaX;
    private int areaY;
    private int areaW;
    private int areaH;

    public MapApp(Font font) {
        this.font = font;
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            centerX = player.getX();
            centerZ = player.getZ();
        }
        requestStatus();
    }

    public static void receive(SatelliteLinkS2CPacket packet) {
        statusKnown = true;
        recon = packet.recon;
        strikeTotal = packet.strikeTotal;
        strikeReady = packet.strikeReady;
        secondsToNext = packet.secondsToNext;
        if (packet.message != null) {
            message = packet.message;
            messageAt = Util.getMillis();
        }
        if (packet.struck) {
            markerX = packet.x;
            markerZ = packet.z;
            ClientLevel level = Minecraft.getInstance().level;
            markerUntil = level == null ? 0L : level.getGameTime() + MARKER_TICKS;
        }
    }

    private static void requestStatus() {
        SkofcraftNetwork.CHANNEL.sendToServer(new SatelliteLinkC2SPacket(SatelliteLinkC2SPacket.STATUS, 0, 0));
    }

    private int scale() {
        return SCALES[scaleIndex];
    }

    private boolean unlocked() {
        return statusKnown && recon > 0;
    }

    @Override
    public void tick() {
        if (ticks++ % STATUS_INTERVAL == 0 && ticks > 1) {
            requestStatus();
        }
        Player player = Minecraft.getInstance().player;
        if (follow && player != null) {
            if (Math.abs(player.getX() - centerX) >= scale() || Math.abs(player.getZ() - centerZ) >= scale()) {
                dirty = true;
            }
            centerX = player.getX();
            centerZ = player.getZ();
        }
        // The world changes too: refresh the picture every couple of seconds even when standing still
        if (dirty || ticks % 40 == 0) {
            redraw();
        }
    }

    // ---------------------------------------------------------------- imagery

    private void ensureTexture(int w, int h) {
        if (texture != null && texW == w && texH == h) {
            return;
        }
        release();
        texW = w;
        texH = h;
        texture = new DynamicTexture(w, h, false);
        Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, texture);
        dirty = true;
    }

    private void redraw() {
        ClientLevel level = Minecraft.getInstance().level;
        if (texture == null || level == null || !unlocked()) {
            return;
        }
        dirty = false;
        NativeImage pixels = texture.getPixels();
        if (pixels == null) {
            return;
        }
        int scale = scale();
        int originX = Mth.floor(centerX) - texW / 2 * scale;
        int originZ = Mth.floor(centerZ) - texH / 2 * scale;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int px = 0; px < texW; px++) {
            int x = originX + px * scale;
            int northHeight = Integer.MIN_VALUE;
            for (int pz = 0; pz < texH; pz++) {
                int z = originZ + pz * scale;
                int height = surface(level, x, z);
                if (northHeight == Integer.MIN_VALUE) {
                    northHeight = surface(level, x, z - scale);
                }
                pixels.setPixelRGBA(px, pz, colorAt(level, pos, x, z, height, northHeight));
                northHeight = height;
            }
        }
        texture.upload();
    }

    private static int surface(ClientLevel level, int x, int z) {
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return Integer.MIN_VALUE + 1;
        }
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
    }

    /** Vanilla map shading: colour of the top block, lighter on slopes rising to the north, darker in deep water. */
    private static int colorAt(ClientLevel level, BlockPos.MutableBlockPos pos, int x, int z, int height, int northHeight) {
        if (height <= level.getMinBuildHeight() - 1 || height == Integer.MIN_VALUE + 1) {
            return UNLOADED;
        }
        int y = height;
        BlockState state = level.getBlockState(pos.set(x, y, z));
        // Look through glass and other blocks that have no map colour
        for (int i = 0; i < 8 && state.getMapColor(level, pos) == MapColor.NONE && y > level.getMinBuildHeight(); i++) {
            state = level.getBlockState(pos.set(x, --y, z));
        }
        MapColor color = state.getMapColor(level, pos);
        MapColor.Brightness brightness;
        FluidState fluid = state.getFluidState();
        if (!fluid.isEmpty() && color == MapColor.WATER) {
            int depth = 0;
            while (depth < 10 && !level.getBlockState(pos.set(x, y - depth - 1, z)).getFluidState().isEmpty()) {
                depth++;
            }
            brightness = depth < 2 ? MapColor.Brightness.HIGH : (depth < 5 ? MapColor.Brightness.NORMAL : MapColor.Brightness.LOW);
        } else if (northHeight == Integer.MIN_VALUE + 1 || height == northHeight) {
            brightness = MapColor.Brightness.NORMAL;
        } else {
            brightness = height > northHeight ? MapColor.Brightness.HIGH : MapColor.Brightness.LOW;
        }
        return color == MapColor.NONE ? UNLOADED : color.calculateRGBColor(brightness);
    }

    // ---------------------------------------------------------------- drawing

    private int mapTop() {
        return areaY + TOOLBAR_H;
    }

    private int mapH() {
        return areaH - TOOLBAR_H - INFO_H;
    }

    @Override
    public void render(GuiGraphics g, Font font, int x, int y, int w, int h, int mouseX, int mouseY) {
        areaX = x;
        areaY = y;
        areaW = w;
        areaH = h;
        g.fill(x, y, x + w, y + h, 0xFF0B0E12);
        if (!statusKnown) {
            drawCentered(g, Component.translatable("phone.skofcraft.map.connecting"), y + h / 2 - 4, MachineScreenHelper.COLOR_TEXT_DIM, 0.8F);
            return;
        }
        if (!unlocked()) {
            drawLocked(g, x, y, w, h);
            return;
        }
        ensureTexture(w, mapH());
        if (dirty) {
            redraw();
        }
        g.blit(TEXTURE_ID, x, mapTop(), 0, 0, texW, texH, texW, texH);
        drawEntities(g, mouseX, mouseY);
        drawMarker(g);
        drawIcbmMark(g);
        drawToolbar(g, mouseX, mouseY);
        drawInfo(g, mouseX, mouseY);
        if (armed && overMap(mouseX, mouseY)) {
            g.fill(mouseX - 6, mouseY, mouseX + 7, mouseY + 1, COLOR_STRIKE);
            g.fill(mouseX, mouseY - 6, mouseX + 1, mouseY + 7, COLOR_STRIKE);
            g.renderOutline(mouseX - 3, mouseY - 3, 7, 7, COLOR_STRIKE);
        }
    }

    private void drawLocked(GuiGraphics g, int x, int y, int w, int h) {
        int cx = x + w / 2;
        int cy = y + h / 2 - 24;
        // A satellite with its panels folded and a red cross
        g.fill(cx - 4, cy - 4, cx + 4, cy + 4, 0xFFC8A030);
        g.fill(cx - 16, cy - 3, cx - 6, cy + 3, 0xFF3A6EC0);
        g.fill(cx + 6, cy - 3, cx + 16, cy + 3, 0xFF3A6EC0);
        g.fill(cx - 6, cy - 1, cx - 4, cy + 1, 0xFF8A9099);
        g.fill(cx + 4, cy - 1, cx + 6, cy + 1, 0xFF8A9099);
        drawCentered(g, Component.translatable("phone.skofcraft.map.locked"), cy + 14, COLOR_STRIKE, 0.9F);
        drawWrapped(g, Component.translatable("phone.skofcraft.map.locked_hint"), x + 8, cy + 28, w - 16, MachineScreenHelper.COLOR_TEXT_DIM);
    }

    private int toScreenX(double worldX) {
        return areaX + (int) Math.round((worldX - centerX) / scale()) + texW / 2;
    }

    private int toScreenY(double worldZ) {
        return mapTop() + (int) Math.round((worldZ - centerZ) / scale()) + texH / 2;
    }

    private double toWorldX(double screenX) {
        return centerX + (screenX - areaX - texW / 2.0D) * scale();
    }

    private double toWorldZ(double screenY) {
        return centerZ + (screenY - mapTop() - texH / 2.0D) * scale();
    }

    private boolean overMap(double mouseX, double mouseY) {
        return mouseX >= areaX && mouseX < areaX + areaW && mouseY >= mapTop() && mouseY < mapTop() + mapH();
    }

    private boolean onMap(int sx, int sy) {
        return sx >= areaX && sx < areaX + texW && sy >= mapTop() && sy < mapTop() + texH;
    }

    private void drawEntities(GuiGraphics g, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Component hover = null;
        for (Entity entity : mc.level.entitiesForRendering()) {
            int color;
            int size;
            if (entity == mc.player) {
                continue;
            } else if (entity instanceof PilotedAircraft) {
                color = 0xFFFF5050;
                size = 2;
            } else if (entity instanceof AircraftCarrier) {
                color = 0xFF50A0FF;
                size = 3;
            } else if (entity instanceof Player player && !player.isSpectator()) {
                color = 0xFFFFFFFF;
                size = 1;
            } else {
                continue;
            }
            int sx = toScreenX(entity.getX());
            int sy = toScreenY(entity.getZ());
            if (!onMap(sx, sy)) {
                continue;
            }
            g.fill(sx - size - 1, sy - size - 1, sx + size + 2, sy + size + 2, 0xFF000000);
            g.fill(sx - size, sy - size, sx + size + 1, sy + size + 1, color);
            if (Math.abs(mouseX - sx) <= size + 2 && Math.abs(mouseY - sy) <= size + 2) {
                hover = entity.getDisplayName();
            }
        }
        if (mc.player != null) {
            drawSelf(g, mc.player);
        }
        if (hover != null) {
            g.renderTooltip(font, hover, mouseX, mouseY);
        }
    }

    /** Yellow marker with a tick showing which way you face. */
    private void drawSelf(GuiGraphics g, Player player) {
        int sx = toScreenX(player.getX());
        int sy = toScreenY(player.getZ());
        if (!onMap(sx, sy)) {
            return;
        }
        double yaw = Math.toRadians(player.getYRot());
        g.fill(sx - 2, sy - 2, sx + 3, sy + 3, 0xFF000000);
        g.fill(sx - 1, sy - 1, sx + 2, sy + 2, 0xFFFFD54A);
        for (int i = 2; i <= 5; i++) {
            int dx = (int) Math.round(-Math.sin(yaw) * i);
            int dz = (int) Math.round(Math.cos(yaw) * i);
            g.fill(sx + dx, sy + dz, sx + dx + 1, sy + dz + 1, 0xFFFFD54A);
        }
    }

    /** Pulsing red rings over a strike target until the rod lands. */
    private void drawMarker(GuiGraphics g) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || level.getGameTime() > markerUntil) {
            return;
        }
        int sx = toScreenX(markerX + 0.5D);
        int sy = toScreenY(markerZ + 0.5D);
        if (!onMap(sx, sy)) {
            return;
        }
        int pulse = (int) (Util.getMillis() / 120L % 6L);
        g.renderOutline(sx - 3 - pulse, sy - 3 - pulse, 7 + pulse * 2, 7 + pulse * 2, COLOR_STRIKE);
        g.fill(sx - 1, sy - 1, sx + 2, sy + 2, COLOR_STRIKE);
    }

    private void drawIcbmMark(GuiGraphics g) {
        if (!icbmMarked) {
            return;
        }
        int sx = toScreenX(icbmX + 0.5D);
        int sy = toScreenY(icbmZ + 0.5D);
        if (!onMap(sx, sy)) {
            return;
        }
        for (int i = -3; i <= 3; i++) {
            g.fill(sx + i, sy + i, sx + i + 1, sy + i + 1, 0xFFC050FF);
            g.fill(sx + i, sy - i, sx + i + 1, sy - i + 1, 0xFFC050FF);
        }
    }

    private void drawToolbar(GuiGraphics g, int mouseX, int mouseY) {
        int y = areaY;
        g.fill(areaX, y, areaX + areaW, y + TOOLBAR_H, 0xFF151A21);
        button(g, areaX + 2, y + 2, 10, "-", mouseX, mouseY, false);
        button(g, areaX + 13, y + 2, 10, "+", mouseX, mouseY, false);
        button(g, areaX + 24, y + 2, 10, "◎", mouseX, mouseY, follow);
        String zoom = "1:" + scale();
        smallText(g, zoom, areaX + 37, y + 4, MachineScreenHelper.COLOR_TEXT_DIM);
        if (strikeTotal > 0) {
            String label = armed ? Component.translatable("phone.skofcraft.map.strike_armed").getString()
                    : Component.translatable("phone.skofcraft.map.strike", strikeReady, strikeTotal).getString();
            int bw = (int) (font.width(label) * 0.7F) + 8;
            int bx = areaX + areaW - bw - 2;
            boolean hot = mouseX >= bx && mouseX < bx + bw && mouseY >= y + 2 && mouseY < y + 12;
            int color = armed ? COLOR_STRIKE : (strikeReady > 0 ? 0xFF8A2A2A : 0xFF3B414C);
            MachineScreenHelper.fillRounded(g, bx, y + 2, bx + bw, y + 12, hot ? 0xFFB83A3A : color);
            smallText(g, label, bx + 4, y + 4, 0xFFFFFFFF);
        }
    }

    private void button(GuiGraphics g, int x, int y, int w, String label, int mouseX, int mouseY, boolean active) {
        boolean hot = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + 10;
        MachineScreenHelper.fillRounded(g, x, y, x + w, y + 10, active ? 0xFF2E6B52 : (hot ? 0xFF4A515E : 0xFF3B414C));
        g.drawCenteredString(font, label, x + w / 2 + 1, y + 1, 0xFFFFFFFF);
    }

    private void drawInfo(GuiGraphics g, int mouseX, int mouseY) {
        int y = areaY + areaH - INFO_H;
        g.fill(areaX, y, areaX + areaW, y + INFO_H, 0xFF151A21);
        String text;
        int color = MachineScreenHelper.COLOR_TEXT_DIM;
        if (message != null && Util.getMillis() - messageAt < MESSAGE_MS) {
            text = message.getString();
            color = message.getStyle().getColor() != null ? 0xFF000000 | message.getStyle().getColor().getValue() : MachineScreenHelper.COLOR_TEXT;
        } else if (overMap(mouseX, mouseY)) {
            text = Component.translatable("phone.skofcraft.map.coords", Mth.floor(toWorldX(mouseX)), Mth.floor(toWorldZ(mouseY))).getString();
        } else if (strikeTotal > 0 && strikeReady == 0) {
            text = Component.translatable("phone.skofcraft.map.recharging", secondsToNext).getString();
        } else {
            text = Component.translatable("phone.skofcraft.map.satellites", recon, strikeTotal).getString();
        }
        smallText(g, font.plainSubstrByWidth(text, (int) ((areaW - 6) / 0.7F)), areaX + 3, y + 2, color);
    }

    private void smallText(GuiGraphics g, String text, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.7F, 0.7F, 1.0F);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private void drawCentered(GuiGraphics g, Component text, int y, int color, float scale) {
        g.pose().pushPose();
        g.pose().translate(areaX + (areaW - font.width(text) * scale) / 2.0F, y, 0);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private void drawWrapped(GuiGraphics g, Component text, int x, int y, int width, int color) {
        float scale = 0.7F;
        int line = 0;
        for (var part : font.split(text, (int) (width / scale))) {
            g.pose().pushPose();
            g.pose().translate(x, y + line * 8, 0);
            g.pose().scale(scale, scale, 1.0F);
            g.drawString(font, part, 0, 0, color, false);
            g.pose().popPose();
            line++;
        }
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!unlocked()) {
            return false;
        }
        if (button == 1 && overMap(mouseX, mouseY)) {
            icbmMarked = true;
            icbmX = Mth.floor(toWorldX(mouseX));
            icbmZ = Mth.floor(toWorldZ(mouseY));
            SkofcraftNetwork.CHANNEL.sendToServer(new SatelliteLinkC2SPacket(SatelliteLinkC2SPacket.MARK, icbmX, icbmZ));
            click(0.7F);
            return true;
        }
        if (button != 0) {
            return false;
        }
        int y = areaY;
        if (mouseY >= y + 2 && mouseY < y + 12) {
            if (mouseX >= areaX + 2 && mouseX < areaX + 12) {
                zoom(1);
                return true;
            }
            if (mouseX >= areaX + 13 && mouseX < areaX + 23) {
                zoom(-1);
                return true;
            }
            if (mouseX >= areaX + 24 && mouseX < areaX + 34) {
                follow = true;
                dirty = true;
                return true;
            }
            if (strikeTotal > 0 && mouseX >= areaX + areaW / 2) {
                armed = !armed;
                click(armed ? 0.6F : 1.0F);
                return true;
            }
        }
        if (armed && overMap(mouseX, mouseY)) {
            armed = false;
            SkofcraftNetwork.CHANNEL.sendToServer(new SatelliteLinkC2SPacket(SatelliteLinkC2SPacket.STRIKE,
                    Mth.floor(toWorldX(mouseX)), Mth.floor(toWorldZ(mouseY))));
            click(0.5F);
            return true;
        }
        return overMap(mouseX, mouseY);
    }

    private static void click(float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SkofcraftSounds.UI_CLICK.get(), pitch));
    }

    @Override
    public boolean mouseDragged(double dragX, double dragY) {
        if (!unlocked() || armed) {
            return false;
        }
        follow = false;
        centerX -= dragX * scale();
        centerZ -= dragY * scale();
        dirty = true;
        return true;
    }

    @Override
    public boolean mouseScrolled(double delta) {
        if (!unlocked()) {
            return false;
        }
        zoom(delta > 0 ? -1 : 1);
        return true;
    }

    /** Positive zooms out (more blocks per pixel). */
    private void zoom(int direction) {
        int next = Mth.clamp(scaleIndex + direction, 0, SCALES.length - 1);
        if (next != scaleIndex) {
            scaleIndex = next;
            dirty = true;
        }
    }

    @Override
    public void onClose() {
        release();
    }

    private void release() {
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(TEXTURE_ID);
            texture = null;
        }
    }
}
