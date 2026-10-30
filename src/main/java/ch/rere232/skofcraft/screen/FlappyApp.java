package ch.rere232.skofcraft.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Flappy Bird clone: tap, click or press Space to flap through the pipes. */
@OnlyIn(Dist.CLIENT)
public class FlappyApp implements PhoneApp {
    private enum State { READY, PLAYING, DEAD }

    private static final float GRAVITY = 620.0F;
    private static final float FLAP_SPEED = -185.0F;
    private static final float PIPE_SPEED = 52.0F;
    private static final float PIPE_SPACING = 84.0F;
    private static final int PIPE_W = 22;
    private static final int GAP = 56;
    private static final int GROUND_H = 18;
    private static final int BIRD_X = 34;
    private static final int BIRD_W = 12;
    private static final int BIRD_H = 9;
    private static final float RESTART_DELAY = 0.5F;

    private static final int SKY_TOP = 0xFF4EC0CA;
    private static final int SKY_BOTTOM = 0xFF9FE3E8;
    private static final int PIPE_GREEN = 0xFF73BF2E;
    private static final int PIPE_DARK = 0xFF4B8A1C;
    private static final int PIPE_LIGHT = 0xFFA6E05A;

    private static int best;

    private static final class Pipe {
        float x;
        final int gapY;
        boolean scored;

        Pipe(float x, int gapY) {
            this.x = x;
            this.gapY = gapY;
        }
    }

    private final Font font;
    private final Random random = new Random();
    private final List<Pipe> pipes = new ArrayList<>();
    private State state = State.READY;
    private float birdY;
    private float velocity;
    private float groundScroll;
    private float cloudScroll;
    private float time;
    private float deadTime;
    private int score;
    private long lastNanos = System.nanoTime();
    private int areaW = PhoneScreen.SCREEN_W;
    private int areaH = 192;

    public FlappyApp(Font font) {
        this.font = font;
        reset();
    }

    private void reset() {
        pipes.clear();
        state = State.READY;
        birdY = areaH / 2.0F - 20;
        velocity = 0.0F;
        score = 0;
        deadTime = 0.0F;
    }

    private int groundTop() {
        return areaH - GROUND_H;
    }

    private void addPipe(float x) {
        int margin = 36;
        int range = groundTop() - margin * 2 - GAP;
        pipes.add(new Pipe(x, margin + GAP / 2 + random.nextInt(Math.max(1, range))));
    }

    @Override
    public void tick() {}

    private void flap() {
        if (state == State.READY) {
            state = State.PLAYING;
            addPipe(areaW + 30);
        }
        if (state == State.PLAYING) {
            velocity = FLAP_SPEED;
        } else if (state == State.DEAD && deadTime > RESTART_DELAY) {
            reset();
        }
    }

    private void simulate(float dt) {
        time += dt;
        cloudScroll += dt * 8.0F;
        switch (state) {
            case READY -> {
                groundScroll += dt * PIPE_SPEED;
                birdY = areaH / 2.0F - 20 + (float) Math.sin(time * 5.0F) * 4.0F;
            }
            case PLAYING -> stepPlaying(dt);
            case DEAD -> {
                deadTime += dt;
                velocity += GRAVITY * dt;
                birdY = Math.min(birdY + velocity * dt, groundTop() - BIRD_H);
            }
        }
    }

    private void stepPlaying(float dt) {
        groundScroll += dt * PIPE_SPEED;
        velocity += GRAVITY * dt;
        birdY += velocity * dt;
        if (birdY < 0.0F) {
            birdY = 0.0F;
            velocity = 0.0F;
        }

        for (Pipe pipe : pipes) {
            pipe.x -= PIPE_SPEED * dt;
            if (!pipe.scored && pipe.x + PIPE_W < BIRD_X) {
                pipe.scored = true;
                score++;
                best = Math.max(best, score);
            }
        }
        pipes.removeIf(pipe -> pipe.x + PIPE_W < -4);
        if (pipes.isEmpty() || pipes.get(pipes.size() - 1).x < areaW - PIPE_SPACING) {
            addPipe(areaW + 4);
        }

        if (hitsSomething()) {
            state = State.DEAD;
            deadTime = 0.0F;
            velocity = Math.max(velocity, 0.0F);
        }
    }

    private boolean hitsSomething() {
        if (birdY + BIRD_H >= groundTop()) {
            birdY = groundTop() - BIRD_H;
            return true;
        }
        float bl = BIRD_X + 1;
        float br = BIRD_X + BIRD_W - 1;
        float bt = birdY + 1;
        float bb = birdY + BIRD_H - 1;
        for (Pipe pipe : pipes) {
            if (br > pipe.x && bl < pipe.x + PIPE_W && (bt < pipe.gapY - GAP / 2 || bb > pipe.gapY + GAP / 2)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(GuiGraphics g, Font font, int x, int y, int w, int h, int mouseX, int mouseY) {
        if (areaH != h || areaW != w) {
            areaW = w;
            areaH = h;
            if (state == State.READY) {
                reset();
            }
        }
        long now = System.nanoTime();
        float dt = Math.min(0.05F, (now - lastNanos) / 1_000_000_000.0F);
        lastNanos = now;
        simulate(dt);

        g.fillGradient(x, y, x + w, y + groundTop(), SKY_TOP, SKY_BOTTOM);
        drawClouds(g, x, y, w);
        for (Pipe pipe : pipes) {
            drawPipe(g, x + Math.round(pipe.x), y, pipe);
        }
        drawGround(g, x, y + groundTop(), w);
        drawBird(g, x + BIRD_X, y + Math.round(birdY));
        drawOverlay(g, x, y, w);
    }

    private void drawClouds(GuiGraphics g, int x, int y, int w) {
        int[][] clouds = {{10, 30, 22}, {70, 14, 28}, {120, 46, 20}, {40, 70, 18}};
        for (int[] c : clouds) {
            int cx = (int) ((c[0] - cloudScroll) % (w + 40));
            if (cx < -c[2]) {
                cx += w + 40;
            }
            int cy = y + c[1];
            g.fill(x + cx, cy + 3, x + cx + c[2], cy + 8, 0xCCFFFFFF);
            g.fill(x + cx + 3, cy, x + cx + c[2] - 5, cy + 3, 0xCCFFFFFF);
        }
    }

    private void drawPipe(GuiGraphics g, int px, int y, Pipe pipe) {
        int gapTop = pipe.gapY - GAP / 2;
        int gapBottom = pipe.gapY + GAP / 2;
        drawPipeBody(g, px + 2, y, PIPE_W - 4, gapTop - 8);
        drawPipeCap(g, px, y + gapTop - 8);
        drawPipeBody(g, px + 2, y + gapBottom + 8, PIPE_W - 4, groundTop() - gapBottom - 8);
        drawPipeCap(g, px, y + gapBottom);
    }

    private void drawPipeBody(GuiGraphics g, int x, int y, int w, int h) {
        if (h <= 0) {
            return;
        }
        g.fill(x, y, x + w, y + h, PIPE_GREEN);
        g.fill(x + 2, y, x + 4, y + h, PIPE_LIGHT);
        g.fill(x + w - 3, y, x + w, y + h, PIPE_DARK);
        g.fill(x, y, x + 1, y + h, 0xFF2D5A0E);
        g.fill(x + w - 1, y, x + w, y + h, 0xFF2D5A0E);
    }

    private void drawPipeCap(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + PIPE_W, y + 8, 0xFF2D5A0E);
        g.fill(x + 1, y + 1, x + PIPE_W - 1, y + 7, PIPE_GREEN);
        g.fill(x + 3, y + 1, x + 6, y + 7, PIPE_LIGHT);
        g.fill(x + PIPE_W - 5, y + 1, x + PIPE_W - 1, y + 7, PIPE_DARK);
    }

    private void drawGround(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + GROUND_H, 0xFFDED895);
        g.fill(x, y, x + w, y + 3, 0xFF73BF2E);
        g.fill(x, y + 3, x + w, y + 4, 0xFF4B8A1C);
        int offset = (int) groundScroll % 8;
        for (int sx = -offset; sx < w; sx += 8) {
            int from = Math.max(0, sx);
            int to = Math.min(w, sx + 4);
            if (to > from) {
                g.fill(x + from, y + 1, x + to, y + 3, 0xFF9AD34A);
            }
        }
        g.fill(x, y + 4, x + w, y + 5, 0xFFC8BF70);
    }

    private void drawBird(GuiGraphics g, int bx, int by) {
        boolean flapUp = state == State.PLAYING ? velocity < 0 : ((int) (time * 8) % 2 == 0);
        g.fill(bx + 1, by, bx + BIRD_W - 2, by + BIRD_H, 0xFF533C1C);
        g.fill(bx, by + 1, bx + BIRD_W - 1, by + BIRD_H - 1, 0xFF533C1C);
        g.fill(bx + 1, by + 1, bx + BIRD_W - 2, by + BIRD_H - 1, 0xFFF8D030);
        g.fill(bx + 1, by + BIRD_H - 3, bx + BIRD_W - 2, by + BIRD_H - 1, 0xFFE8A820);
        g.fill(bx + 7, by + 1, bx + 10, by + 4, 0xFFFFFFFF);
        g.fill(bx + 9, by + 2, bx + 10, by + 3, 0xFF000000);
        g.fill(bx + 8, by + 5, bx + BIRD_W + 1, by + 8, 0xFFE0502C);
        g.fill(bx + 8, by + 6, bx + BIRD_W, by + 7, 0xFF533C1C);
        int wy = flapUp ? by + 2 : by + 4;
        g.fill(bx + 1, wy, bx + 6, wy + 3, 0xFFFFFFFF);
        g.fill(bx + 1, wy + 2, bx + 6, wy + 3, 0xFFC8C8C8);
    }

    private void drawOverlay(GuiGraphics g, int x, int y, int w) {
        if (state != State.READY) {
            drawScore(g, Integer.toString(score), x + w / 2, y + 10, 2.0F);
        }
        if (state == State.READY) {
            drawCentered(g, Component.translatable("phone.skofcraft.flappy.title").getString(), x + w / 2, y + 22, 1.4F, 0xFFFFFFFF);
            if ((int) (time * 2) % 2 == 0) {
                drawCentered(g, Component.translatable("phone.skofcraft.flappy.tap").getString(), x + w / 2, y + areaH / 2 + 12, 0.8F, 0xFFFFFFFF);
            }
        } else if (state == State.DEAD) {
            int px = x + 18;
            int pw = w - 36;
            int py = y + areaH / 2 - 40;
            g.fill(px - 1, py - 1, px + pw + 1, py + 52, 0xFF533C1C);
            g.fill(px, py, px + pw, py + 51, 0xFFDED895);
            drawCentered(g, Component.translatable("phone.skofcraft.flappy.over").getString(), x + w / 2, py + 4, 1.3F, 0xFFE0502C);
            drawCentered(g, Component.translatable("phone.skofcraft.flappy.score", score).getString(), x + w / 2, py + 20, 0.9F, 0xFF533C1C);
            drawCentered(g, Component.translatable("phone.skofcraft.flappy.best", best).getString(), x + w / 2, py + 30, 0.9F, 0xFF533C1C);
            if (deadTime > RESTART_DELAY) {
                drawCentered(g, Component.translatable("phone.skofcraft.flappy.retry").getString(), x + w / 2, py + 41, 0.7F, 0xFF7A6A3A);
            }
        }
    }

    private void drawScore(GuiGraphics g, String text, int cx, int y, float scale) {
        drawCentered(g, text, cx + 1, y + 1, scale, 0xFF000000);
        drawCentered(g, text, cx, y, scale, 0xFFFFFFFF);
    }

    private void drawCentered(GuiGraphics g, String text, int cx, int y, float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(cx - font.width(text) * scale / 2.0F, y, 0);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            flap();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode) {
        // space or up arrow
        if (keyCode == 32 || keyCode == 265) {
            flap();
            return true;
        }
        return false;
    }
}
