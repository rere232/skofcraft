package ch.rere232.skofcraft.screen;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.casino.Roulette;
import ch.rere232.skofcraft.casino.SlotMachine;
import ch.rere232.skofcraft.network.RouletteSpinC2SPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Roulette: a number strip scrolls past a pointer and slows down onto the server's number. */
@OnlyIn(Dist.CLIENT)
final class RouletteGame extends CasinoGame {
    private static final int BUTTON_TYPE = 0;
    private static final int BUTTON_MINUS = 20;
    private static final int BUTTON_PLUS = 21;
    private static final int BUTTON_SPIN = 22;
    private static final int POCKETS = Roulette.WHEEL.length;
    private static final int CELL_W = 20;
    private static final int STRIP_H = 24;
    private static final float WAIT_SPEED = 14.0F;
    private static final float ANIM_TIME = 3.2F;
    private static final float TIMEOUT = 4.0F;
    private static final int HISTORY = 8;

    private static int selectedType;
    private static int pickedNumber = 17;

    private final List<Integer> history = new ArrayList<>();
    private float stripPos;
    private boolean waiting;
    private boolean animating;
    private float waitTime;
    private float animTime;
    private float animStart;
    private float animEnd;
    private int number = -1;
    private int lastBet;
    private int lastPayout;
    private boolean timedOut;
    private float flash;
    private int lastCell;
    private float tickCooldown;

    RouletteGame(CasinoApp host, Font font) {
        super(host, font);
    }

    @Override
    boolean isBusy() {
        return waiting || animating;
    }

    boolean isWaiting() {
        return waiting;
    }

    void receive(int result, int bet, int payout) {
        waiting = false;
        animating = true;
        animTime = 0.0F;
        number = result;
        lastBet = bet;
        lastPayout = payout;

        int index = 0;
        for (int i = 0; i < POCKETS; i++) {
            if (Roulette.WHEEL[i] == result) {
                index = i;
            }
        }
        int base = Mth.floor(stripPos);
        animStart = stripPos;
        float jitter = (host.random().nextFloat() - 0.5F) * 0.7F;
        animEnd = base + POCKETS * 3 + Math.floorMod(index - base, POCKETS) + jitter;
    }

    private void startSpin() {
        if (isBusy() || !host.canAfford(SlotMachine.BETS[CasinoApp.selectedBet])) {
            return;
        }
        waiting = true;
        timedOut = false;
        waitTime = 0.0F;
        number = -1;
        host.freezeDiamonds();
        SkofcraftNetwork.CHANNEL.sendToServer(
                new RouletteSpinC2SPacket(CasinoApp.selectedBet, selectedType, pickedNumber));
        host.play(SkofcraftSounds.UI_CLICK.get(), 1.0F);
    }

    private void step(float dt) {
        flash += dt;
        tickCooldown -= dt;
        if (waiting) {
            waitTime += dt;
            stripPos += WAIT_SPEED * dt;
            if (waitTime > TIMEOUT) {
                waiting = false;
                timedOut = true;
                host.unfreezeDiamonds();
            }
        } else if (animating) {
            animTime += dt;
            float t = Math.min(1.0F, animTime / ANIM_TIME);
            float eased = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
            stripPos = animStart + (animEnd - animStart) * eased;
            if (t >= 1.0F) {
                animating = false;
                history.add(0, number);
                if (history.size() > HISTORY) {
                    history.remove(history.size() - 1);
                }
                host.unfreezeDiamonds();
                if (lastPayout > 0) {
                    host.play(SkofcraftSounds.COIN.get(), 1.0F);
                }
            }
        }
        int cell = Mth.floor(stripPos + 0.5F);
        if ((waiting || animating) && cell != lastCell && tickCooldown <= 0.0F) {
            host.play(SkofcraftSounds.UI_CLICK.get(), 1.6F);
            tickCooldown = 0.06F;
        }
        lastCell = cell;
    }

    @Override
    void render(GuiGraphics g, int x, int y, int w, int h, float dt) {
        step(dt);
        drawStrip(g, x + 6, y, w - 12);
        drawHistory(g, x + 6, y + 28);
        drawResultText(g, x + w / 2, y + 40);

        boolean idle = !isBusy();
        int bw = (w - 12 - 8) / 3;
        for (int i = 0; i < 6; i++) {
            int bx = x + 6 + (i % 3) * (bw + 4);
            int by = y + 52 + (i / 3) * 16;
            int bg = i == Roulette.TYPE_RED ? RED : (i == Roulette.TYPE_BLACK ? 0xFF1B1B1F : 0xFF2A1A1E);
            button(g, BUTTON_TYPE + i, bx, by, bw, 14, typeName(i), bg, 0xFFFFFFFF, idle, selectedType == i);
        }

        int ny = y + 84;
        button(g, BUTTON_TYPE + Roulette.TYPE_NUMBER, x + 6, ny, bw, 14, typeName(Roulette.TYPE_NUMBER),
                0xFF2A1A1E, 0xFFFFFFFF, idle, selectedType == Roulette.TYPE_NUMBER);
        button(g, BUTTON_MINUS, x + 6 + bw + 4, ny, 14, 14, Component.literal("-"), 0xFF3A404C, 0xFFFFFFFF, idle, false);
        int numberColor = pickedNumber == 0 ? 0xFF1F8A4C : (Roulette.isRed(pickedNumber) ? RED : 0xFF1B1B1F);
        MachineScreenHelper.fillRounded(g, x + 6 + bw + 20, ny, x + 6 + bw + 20 + 24, ny + 14, numberColor);
        drawCentered(g, Component.literal(Integer.toString(pickedNumber)), x + 6 + bw + 32, ny + 3, 0.9F, 0xFFFFFFFF);
        button(g, BUTTON_PLUS, x + 6 + bw + 48, ny, 14, 14, Component.literal("+"), 0xFF3A404C, 0xFFFFFFFF, idle, false);

        betRow(g, x, y + 104, w, idle);

        boolean enabled = idle && host.canAfford(SlotMachine.BETS[CasinoApp.selectedBet]);
        int sw = 80;
        int sx = x + (w - sw) / 2;
        int sy = y + 124;
        MachineScreenHelper.fillRounded(g, sx, sy + 1, sx + sw, sy + 18, enabled ? 0xFF7A1521 : 0xFF3A404C);
        button(g, BUTTON_SPIN, sx, sy, sw, 17, Component.translatable("phone.skofcraft.casino.spin")
                .withStyle(ChatFormatting.BOLD), enabled ? RED : 0xFF2A2F38, 0xFFFFFFFF, enabled, false);
        drawSmall(g, Component.translatable("phone.skofcraft.casino.roulette.odds").getString(), x + 6, y + 146, DIM);
    }

    private Component typeName(int type) {
        return Component.translatable("phone.skofcraft.casino.roulette.type." + type);
    }

    private static int pocketColor(int number) {
        if (number == 0) {
            return 0xFF1F8A4C;
        }
        return Roulette.isRed(number) ? RED : 0xFF1B1B1F;
    }

    private void drawStrip(GuiGraphics g, int x, int y, int w) {
        g.fill(x - 1, y - 1, x + w + 1, y + STRIP_H + 1, GOLD);
        g.fill(x, y, x + w, y + STRIP_H, 0xFF0D0508);
        int cx = x + w / 2;
        g.enableScissor(x, y, x + w, y + STRIP_H);
        int base = Mth.floor(stripPos);
        for (int k = base - 4; k <= base + 5; k++) {
            int n = Roulette.WHEEL[Math.floorMod(k, POCKETS)];
            int left = Math.round(cx + (k - stripPos) * CELL_W - CELL_W / 2.0F);
            g.fill(left + 1, y + 2, left + CELL_W - 1, y + STRIP_H - 2, pocketColor(n));
            String label = Integer.toString(n);
            g.pose().pushPose();
            g.pose().translate(left + (CELL_W - font.width(label) * 0.9F) / 2.0F, y + 8, 0);
            g.pose().scale(0.9F, 0.9F, 1.0F);
            g.drawString(font, label, 0, 0, 0xFFFFFFFF, false);
            g.pose().popPose();
        }
        g.fillGradient(x, y, x + 18, y + STRIP_H, 0xCC0D0508, 0x000D0508);
        g.fill(cx - 1, y, cx + 1, y + STRIP_H, 0xFFFFFFFF);
        g.disableScissor();
        g.fill(cx - 3, y - 1, cx + 3, y, GOLD);
        g.fill(cx - 3, y + STRIP_H, cx + 3, y + STRIP_H + 1, GOLD);
    }

    private void drawHistory(GuiGraphics g, int x, int y) {
        for (int i = 0; i < history.size(); i++) {
            int n = history.get(i);
            int cx = x + i * 14;
            g.fill(cx, y, cx + 13, y + 8, pocketColor(n));
            String label = Integer.toString(n);
            g.pose().pushPose();
            g.pose().translate(cx + (13 - font.width(label) * 0.6F) / 2.0F, y + 1.5F, 0);
            g.pose().scale(0.6F, 0.6F, 1.0F);
            g.drawString(font, label, 0, 0, 0xFFFFFFFF, false);
            g.pose().popPose();
        }
    }

    private void drawResultText(GuiGraphics g, int cx, int y) {
        Component text;
        int color;
        if (waiting || animating) {
            text = Component.translatable("phone.skofcraft.casino.roulette.spinning");
            color = DIM;
        } else if (timedOut) {
            text = Component.translatable("phone.skofcraft.casino.timeout");
            color = DIM;
        } else if (number < 0) {
            text = Component.translatable("phone.skofcraft.casino.place");
            color = DIM;
        } else if (lastPayout > 0) {
            text = Component.translatable("phone.skofcraft.casino.roulette.win", number, lastPayout);
            color = lastPayout >= lastBet * 36 && (int) (flash * 8) % 2 == 0 ? 0xFFFFFFFF : 0xFF3DDC97;
        } else {
            text = Component.translatable("phone.skofcraft.casino.roulette.lose", number);
            color = 0xFFE5484D;
        }
        drawCentered(g, text, cx, y, 0.85F, color);
    }

    @Override
    protected void onButton(int id) {
        if (id >= BUTTON_TYPE && id < BUTTON_TYPE + Roulette.TYPE_COUNT) {
            selectedType = id - BUTTON_TYPE;
            host.play(SkofcraftSounds.UI_CLICK.get(), 1.2F);
        } else if (id == BUTTON_MINUS || id == BUTTON_PLUS) {
            pickedNumber = Math.floorMod(pickedNumber + (id == BUTTON_PLUS ? 1 : -1), 37);
            selectedType = Roulette.TYPE_NUMBER;
            host.play(SkofcraftSounds.UI_CLICK.get(), 1.4F);
        } else if (id == BUTTON_SPIN) {
            startSpin();
        }
    }

    @Override
    boolean keyPressed(int keyCode) {
        if (keyCode == 32) {
            startSpin();
            return true;
        }
        return false;
    }
}
