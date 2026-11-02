package ch.rere232.skofcraft.screen;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.item.SkofeeItem;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import ch.rere232.skofcraft.network.SkofeeBrewC2SPacket;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

/**
 * SKOFee Machine minigame: stop a moving needle in the green zone to tamp the grounds and heat the water,
 * then stop the pour at the line. The average score sets the cup quality.
 */
@OnlyIn(Dist.CLIENT)
public class SkofeeBrewScreen extends Screen {
    private static final int PANEL_W = 220;
    private static final int PANEL_H = 150;
    private static final int BAR_W = 180;
    private static final int BAR_H = 14;
    private static final int CUP_W = 40;
    private static final int CUP_H = 60;
    private static final int STEPS = 3;
    private static final int STEP_POUR = 2;
    private static final long RESULT_MS = 800L;
    private static final String[] STEP_KEYS = {"tamp", "heat", "pour"};

    private static final int COLOR_ZONE = 0xFF2E6B52;
    private static final int COLOR_SKOFEE = 0xFF5A321A;
    private static final int COLOR_CREMA = 0xFFC8955A;
    private static final int COLOR_MISS = 0xFFE05555;
    private static final int COLOR_PERFECT = 0xFFFFD54A;

    private final BlockPos pos;
    private final RandomSource random = RandomSource.create();
    private final int[] scores = new int[STEPS];
    private int step = -1;
    private long stepStart;
    /** Time the needle was stopped in the current step, or -1 while it is still moving. */
    private long stoppedAt = -1L;
    private float stoppedValue;
    private float target;
    private float zoneHalf;
    private boolean finished;
    private Button serveButton;
    private int leftPos;
    private int topPos;

    public SkofeeBrewScreen(BlockPos pos) {
        super(Component.translatable("block.skofcraft.skofee_machine"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (this.width - PANEL_W) / 2;
        topPos = (this.height - PANEL_H) / 2;
        if (step < 0) {
            startStep(0);
        }
        serveButton = addRenderableWidget(Button.builder(Component.translatable("screen.skofcraft.skofee.serve"), button -> serve())
                .bounds(leftPos + PANEL_W / 2 - 40, topPos + PANEL_H - 30, 80, 20).build());
        serveButton.visible = finished;
    }

    private void startStep(int newStep) {
        step = newStep;
        stepStart = Util.getMillis();
        stoppedAt = -1L;
        switch (step) {
            case 0 -> {
                target = 0.25F + random.nextFloat() * 0.5F;
                zoneHalf = 0.10F;
            }
            case 1 -> {
                target = 0.2F + random.nextFloat() * 0.6F;
                zoneHalf = 0.065F;
            }
            default -> {
                target = 0.68F + random.nextFloat() * 0.18F;
                zoneHalf = 0.04F;
            }
        }
    }

    /** Needle position (0-1) for the current step; for the pour it is the cup fill and can go past 1. */
    private float currentValue() {
        if (stoppedAt >= 0L) {
            return stoppedValue;
        }
        float t = (Util.getMillis() - stepStart) / 1000.0F;
        return switch (step) {
            case 0 -> pingPong(t * 0.8F);
            case 1 -> pingPong(t * 1.3F);
            default -> t * 0.18F + t * t * 0.12F;
        };
    }

    private static float pingPong(float x) {
        float f = x - (float) Math.floor(x);
        return f < 0.5F ? f * 2.0F : 2.0F - f * 2.0F;
    }

    private int scoreFor(float value) {
        if (step == STEP_POUR && value >= 1.0F) {
            return 0;
        }
        float distance = Math.abs(value - target);
        if (distance <= zoneHalf) {
            return 100 - Math.round(distance / zoneHalf * 20.0F);
        }
        return Math.max(0, 80 - Math.round((distance - zoneHalf) * 400.0F));
    }

    private void stop() {
        if (finished || stoppedAt >= 0L) {
            return;
        }
        float value = currentValue();
        stoppedValue = Math.min(1.0F, value);
        stoppedAt = Util.getMillis();
        int score = scoreFor(value);
        scores[step] = score;
        Minecraft.getInstance().getSoundManager().play(score >= 50
                ? SimpleSoundInstance.forUI(SkofcraftSounds.COFFEE_DING.get(), 0.8F + score / 100.0F)
                : SimpleSoundInstance.forUI(SkofcraftSounds.COFFEE_FAIL.get(), 0.7F));
    }

    private int totalScore() {
        int sum = 0;
        for (int score : scores) {
            sum += score;
        }
        return Math.round(sum / (float) STEPS);
    }

    private void serve() {
        if (finished) {
            SkofcraftNetwork.CHANNEL.sendToServer(new SkofeeBrewC2SPacket(pos, totalScore()));
            onClose();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (finished) {
            return;
        }
        if (stoppedAt < 0L && step == STEP_POUR && currentValue() >= 1.0F) {
            stop();
        } else if (stoppedAt >= 0L && Util.getMillis() - stoppedAt >= RESULT_MS) {
            if (step + 1 < STEPS) {
                startStep(step + 1);
            } else {
                finished = true;
                serveButton.visible = true;
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER) {
            if (finished) {
                serve();
            } else {
                stop();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0 && !finished && mouseX >= leftPos && mouseX < leftPos + PANEL_W && mouseY >= topPos && mouseY < topPos + PANEL_H) {
            stop();
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        MachineScreenHelper.drawPanel(g, leftPos, topPos, PANEL_W, PANEL_H);
        g.drawString(this.font, this.title, leftPos + 8, topPos + 5, MachineScreenHelper.COLOR_TEXT, false);
        renderStepDots(g);

        if (finished) {
            renderResult(g);
        } else {
            Component stepName = Component.translatable("screen.skofcraft.skofee.step." + STEP_KEYS[step]);
            g.drawCenteredString(this.font, Component.translatable("screen.skofcraft.skofee.step", step + 1, STEPS, stepName),
                    leftPos + PANEL_W / 2, topPos + 26, MachineScreenHelper.COLOR_ACCENT);
            if (step == STEP_POUR) {
                renderCup(g);
            } else {
                renderGauge(g);
            }
            if (stoppedAt >= 0L) {
                renderRating(g);
            } else {
                g.drawCenteredString(this.font, Component.translatable(step == STEP_POUR ? "screen.skofcraft.skofee.hint_pour" : "screen.skofcraft.skofee.hint"),
                        leftPos + PANEL_W / 2, topPos + PANEL_H - 16, MachineScreenHelper.COLOR_TEXT_DIM);
            }
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderStepDots(GuiGraphics g) {
        for (int i = 0; i < STEPS; i++) {
            int x = leftPos + PANEL_W - 14 - (STEPS - 1 - i) * 10;
            int y = topPos + 6;
            int color;
            if (i < step || (i == step && stoppedAt >= 0L) || finished) {
                color = ratingColor(scores[i]);
            } else if (i == step) {
                color = MachineScreenHelper.COLOR_TEXT;
            } else {
                color = MachineScreenHelper.COLOR_HIGHLIGHT;
            }
            g.fill(x, y, x + 6, y + 6, color);
        }
    }

    private void renderGauge(GuiGraphics g) {
        int x = leftPos + (PANEL_W - BAR_W) / 2;
        int y = topPos + 54;
        MachineScreenHelper.fillRounded(g, x - 2, y - 2, x + BAR_W + 2, y + BAR_H + 2, MachineScreenHelper.COLOR_WELL);
        int zoneStart = x + Math.round((target - zoneHalf) * BAR_W);
        int zoneEnd = x + Math.round((target + zoneHalf) * BAR_W);
        g.fill(zoneStart, y, zoneEnd, y + BAR_H, COLOR_ZONE);
        int sweetHalf = Math.max(1, Math.round(zoneHalf * 0.3F * BAR_W));
        int center = x + Math.round(target * BAR_W);
        g.fill(center - sweetHalf, y, center + sweetHalf, y + BAR_H, MachineScreenHelper.COLOR_PROGRESS);

        int needle = x + Math.round(currentValue() * BAR_W);
        g.fill(needle - 1, y - 4, needle + 1, y + BAR_H + 4, MachineScreenHelper.COLOR_TEXT);

        String key = STEP_KEYS[step];
        g.drawString(this.font, Component.translatable("screen.skofcraft.skofee." + key + ".low"), x, y + BAR_H + 7, MachineScreenHelper.COLOR_TEXT_DIM, false);
        Component high = Component.translatable("screen.skofcraft.skofee." + key + ".high");
        g.drawString(this.font, high, x + BAR_W - this.font.width(high), y + BAR_H + 7, MachineScreenHelper.COLOR_TEXT_DIM, false);
    }

    private void renderCup(GuiGraphics g) {
        int x = leftPos + (PANEL_W - CUP_W) / 2;
        int y = topPos + 40;
        float fill = Math.min(1.0F, currentValue());
        // Cup walls and handle
        MachineScreenHelper.fillRounded(g, x - 3, y, x + CUP_W + 3, y + CUP_H + 3, MachineScreenHelper.COLOR_HIGHLIGHT);
        g.fill(x + CUP_W + 3, y + 14, x + CUP_W + 10, y + 18, MachineScreenHelper.COLOR_HIGHLIGHT);
        g.fill(x + CUP_W + 7, y + 18, x + CUP_W + 10, y + 34, MachineScreenHelper.COLOR_HIGHLIGHT);
        g.fill(x + CUP_W + 3, y + 34, x + CUP_W + 10, y + 38, MachineScreenHelper.COLOR_HIGHLIGHT);
        g.fill(x, y, x + CUP_W, y + CUP_H, MachineScreenHelper.COLOR_WELL);

        int zoneTop = y + CUP_H - Math.round((target + zoneHalf) * CUP_H);
        int zoneBottom = y + CUP_H - Math.round((target - zoneHalf) * CUP_H);
        g.fill(x, zoneTop, x + CUP_W, zoneBottom, COLOR_ZONE);

        int level = Math.round(fill * CUP_H);
        if (level > 0) {
            g.fill(x, y + CUP_H - level, x + CUP_W, y + CUP_H, COLOR_SKOFEE);
            g.fill(x, y + CUP_H - level, x + CUP_W, y + CUP_H - level + Math.min(2, level), COLOR_CREMA);
        }

        // Stream from the machine while pouring
        if (stoppedAt < 0L) {
            g.fill(x + CUP_W / 2 - 1, topPos + 36, x + CUP_W / 2 + 1, y + CUP_H - level, COLOR_SKOFEE);
        }

        int line = y + CUP_H - Math.round(target * CUP_H);
        g.fill(x - 8, line, x, line + 1, MachineScreenHelper.COLOR_ACCENT);
        g.fill(x + CUP_W, line, x + CUP_W + 3, line + 1, MachineScreenHelper.COLOR_ACCENT);
    }

    private void renderRating(GuiGraphics g) {
        int score = scores[step];
        String key;
        if (step == STEP_POUR && stoppedValue >= 1.0F) {
            key = "overflow";
        } else if (score >= 95) {
            key = "perfect";
        } else if (score >= 80) {
            key = "great";
        } else if (score >= 50) {
            key = "okay";
        } else {
            key = "miss";
        }
        g.pose().pushPose();
        g.pose().translate(leftPos + PANEL_W / 2.0F, topPos + PANEL_H - 22, 0.0F);
        g.pose().scale(1.5F, 1.5F, 1.0F);
        g.drawCenteredString(this.font, Component.translatable("screen.skofcraft.skofee.rating." + key), 0, 0, ratingColor(score));
        g.pose().popPose();
    }

    private void renderResult(GuiGraphics g) {
        int score = totalScore();
        int quality = SkofeeItem.qualityFromScore(score);
        int centerX = leftPos + PANEL_W / 2;
        g.drawCenteredString(this.font, Component.translatable("screen.skofcraft.skofee.result"), centerX, topPos + 34, MachineScreenHelper.COLOR_TEXT);
        g.pose().pushPose();
        g.pose().translate(centerX, topPos + 52, 0.0F);
        g.pose().scale(2.0F, 2.0F, 1.0F);
        g.drawCenteredString(this.font, SkofeeItem.qualityName(quality), 0, 0, MachineScreenHelper.COLOR_TEXT);
        g.pose().popPose();
        g.drawCenteredString(this.font, Component.translatable("screen.skofcraft.skofee.score", score), centerX, topPos + 80, MachineScreenHelper.COLOR_TEXT_DIM);
        g.drawCenteredString(this.font, Component.translatable("screen.skofcraft.skofee.effect." + quality), centerX, topPos + 94, MachineScreenHelper.COLOR_TEXT_DIM);
    }

    private static int ratingColor(int score) {
        if (score >= 95) {
            return COLOR_PERFECT;
        }
        if (score >= 80) {
            return MachineScreenHelper.COLOR_PROGRESS;
        }
        return score >= 50 ? MachineScreenHelper.COLOR_ACCENT : COLOR_MISS;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
