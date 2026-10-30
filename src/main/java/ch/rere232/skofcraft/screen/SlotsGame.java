package ch.rere232.skofcraft.screen;

import ch.rere232.skofcraft.casino.SlotMachine;
import ch.rere232.skofcraft.network.CasinoSpinC2SPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Three-reel slot machine. */
@OnlyIn(Dist.CLIENT)
final class SlotsGame extends CasinoGame {
    private static final int BUTTON_SPIN = 1;
    private static final int REEL_W = 32;
    private static final int REEL_H = 40;
    private static final int REEL_GAP = 6;
    private static final float CELL = 26.0F;
    private static final float SPIN_SPEED = 14.0F;
    private static final float MIN_SPIN = 0.7F;
    private static final float REEL_DELAY = 0.45F;
    private static final float TIMEOUT = 4.0F;

    private final float[] pos = new float[SlotMachine.REELS];
    private final int[] target = new int[SlotMachine.REELS];
    private final float[] stopAt = new float[SlotMachine.REELS];
    private final boolean[] stopped = new boolean[SlotMachine.REELS];

    private boolean busy;
    private boolean waiting;
    private float spinTime;
    private float flash;
    private int lastBet;
    private int lastPayout = -1;
    private boolean jackpot;
    private boolean timedOut;

    SlotsGame(CasinoApp host, Font font) {
        super(host, font);
        for (int i = 0; i < pos.length; i++) {
            pos[i] = i * 2;
        }
    }

    boolean isWaiting() {
        return waiting;
    }

    @Override
    boolean isBusy() {
        return busy;
    }

    void receive(int[] reels, int bet, int payout) {
        waiting = false;
        lastBet = bet;
        lastPayout = payout;
        jackpot = reels[0] == reels[1] && reels[1] == reels[2] && reels[0] == SlotMachine.JACKPOT_SYMBOL;
        for (int i = 0; i < target.length; i++) {
            target[i] = reels[i];
            stopAt[i] = Math.max(spinTime, MIN_SPIN) + REEL_DELAY * i;
        }
    }

    private void startSpin() {
        if (busy || !host.canAfford(SlotMachine.BETS[CasinoApp.selectedBet])) {
            return;
        }
        busy = true;
        waiting = true;
        timedOut = false;
        spinTime = 0.0F;
        lastPayout = -1;
        jackpot = false;
        host.freezeDiamonds();
        for (int i = 0; i < stopped.length; i++) {
            stopped[i] = false;
        }
        SkofcraftNetwork.CHANNEL.sendToServer(new CasinoSpinC2SPacket(CasinoApp.selectedBet));
        host.play(SoundEvents.UI_BUTTON_CLICK.get(), 1.0F);
    }

    private void step(float dt) {
        flash += dt;
        if (!busy) {
            return;
        }
        spinTime += dt;
        if (waiting && spinTime > TIMEOUT) {
            busy = false;
            waiting = false;
            timedOut = true;
            host.unfreezeDiamonds();
            return;
        }
        boolean allStopped = true;
        for (int i = 0; i < pos.length; i++) {
            if (stopped[i]) {
                continue;
            }
            if (!waiting && spinTime >= stopAt[i]) {
                stopped[i] = true;
                pos[i] = target[i];
                host.play(SoundEvents.UI_BUTTON_CLICK.get(), 0.8F + i * 0.2F);
            } else {
                pos[i] += SPIN_SPEED * dt;
                allStopped = false;
            }
        }
        if (allStopped && !waiting) {
            busy = false;
            host.unfreezeDiamonds();
            if (jackpot) {
                host.play(SoundEvents.PLAYER_LEVELUP, 1.0F);
            } else if (lastPayout > lastBet) {
                host.play(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F);
            }
        }
    }

    @Override
    void render(GuiGraphics g, int x, int y, int w, int h, float dt) {
        step(dt);
        drawMachine(g, x, y, w);
        betRow(g, x, y + 79, w, !busy);

        boolean enabled = !busy && host.canAfford(SlotMachine.BETS[CasinoApp.selectedBet]);
        int sw = 80;
        int sx = x + (w - sw) / 2;
        int sy = y + 101;
        MachineScreenHelper.fillRounded(g, sx, sy + 1, sx + sw, sy + 18, enabled ? 0xFF7A1521 : 0xFF3A404C);
        button(g, BUTTON_SPIN, sx, sy, sw, 17, Component.translatable("phone.skofcraft.casino.spin")
                .withStyle(ChatFormatting.BOLD), enabled ? RED : 0xFF2A2F38, 0xFFFFFFFF, enabled, false);
        drawPayTable(g, x, y + 127, w);
    }

    private void drawMachine(GuiGraphics g, int x, int y, int w) {
        int top = y + 2;
        MachineScreenHelper.fillRounded(g, x + 6, top, x + w - 6, top + 72, GOLD);
        MachineScreenHelper.fillRounded(g, x + 8, top + 2, x + w - 8, top + 70, 0xFF3A1018);

        int reelsW = SlotMachine.REELS * REEL_W + (SlotMachine.REELS - 1) * REEL_GAP;
        int rx = x + (w - reelsW) / 2;
        int ry = top + 8;
        boolean win = !busy && lastPayout > lastBet;
        int frame = win && ((int) (flash * 6) % 2 == 0) ? 0xFFFFFFFF : (win ? GOLD : 0xFF0D0508);

        for (int i = 0; i < SlotMachine.REELS; i++) {
            int wx = rx + i * (REEL_W + REEL_GAP);
            MachineScreenHelper.fillRounded(g, wx - 1, ry - 1, wx + REEL_W + 1, ry + REEL_H + 1, frame);
            g.fill(wx, ry, wx + REEL_W, ry + REEL_H, 0xFFF3EBD6);
            g.fillGradient(wx, ry, wx + REEL_W, ry + 8, 0x66000000, 0x00000000);
            g.fillGradient(wx, ry + REEL_H - 8, wx + REEL_W, ry + REEL_H, 0x00000000, 0x66000000);
            g.enableScissor(wx, ry, wx + REEL_W, ry + REEL_H);
            drawReel(g, wx, ry, pos[i]);
            g.disableScissor();
        }

        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        g.fill(rx - 4, ry + REEL_H / 2, rx + reelsW + 4, ry + REEL_H / 2 + 1, 0xCCC8283A);
        g.pose().popPose();

        drawResultText(g, x + w / 2, top + 54);
    }

    private void drawReel(GuiGraphics g, int wx, int ry, float p) {
        int base = Mth.floor(p);
        int count = SlotMachine.SYMBOLS.size();
        for (int k = base - 1; k <= base + 2; k++) {
            float cy = ry + REEL_H / 2.0F + (p - k) * CELL;
            ItemStack stack = new ItemStack(SlotMachine.SYMBOLS.get(Math.floorMod(k, count)).item().get());
            g.pose().pushPose();
            g.pose().translate(wx + REEL_W / 2.0F - 12.0F, cy - 12.0F, 0);
            g.pose().scale(1.5F, 1.5F, 1.0F);
            g.renderItem(stack, 0, 0);
            g.pose().popPose();
        }
    }

    private void drawResultText(GuiGraphics g, int cx, int y) {
        Component text;
        int color;
        if (busy) {
            text = Component.translatable("phone.skofcraft.casino.spinning");
            color = DIM;
        } else if (timedOut) {
            text = Component.translatable("phone.skofcraft.casino.timeout");
            color = DIM;
        } else if (lastPayout < 0) {
            text = Component.translatable("phone.skofcraft.casino.place");
            color = DIM;
        } else if (jackpot) {
            text = Component.translatable("phone.skofcraft.casino.jackpot", lastPayout);
            color = (int) (flash * 8) % 2 == 0 ? 0xFFFFFFFF : GOLD;
        } else if (lastPayout > lastBet) {
            text = Component.translatable("phone.skofcraft.casino.win", lastPayout);
            color = 0xFF3DDC97;
        } else if (lastPayout == lastBet) {
            text = Component.translatable("phone.skofcraft.casino.push");
            color = 0xFFD5D8DD;
        } else {
            text = Component.translatable("phone.skofcraft.casino.lose");
            color = 0xFFE5484D;
        }
        drawCentered(g, text, cx, y, 0.85F, color);
    }

    private void drawPayTable(GuiGraphics g, int x, int top, int w) {
        int colW = w / SlotMachine.SYMBOLS.size();
        for (int i = 0; i < SlotMachine.SYMBOLS.size(); i++) {
            SlotMachine.Symbol symbol = SlotMachine.SYMBOLS.get(i);
            int cx = x + i * colW + 4;
            g.pose().pushPose();
            g.pose().translate(cx, top, 0);
            g.pose().scale(0.5F, 0.5F, 1.0F);
            g.renderItem(new ItemStack(symbol.item().get()), 0, 0);
            g.pose().popPose();
            drawSmall(g, "x" + symbol.multiplier(), cx + 10, top + 2, GOLD);
        }
        drawSmall(g, Component.translatable("phone.skofcraft.casino.pair").getString(), x + 6, top + 12, DIM);
        drawSmall(g, Component.translatable("phone.skofcraft.casino.warning").getString(), x + 6, top + 22, 0xFF7A5A60);
    }

    @Override
    protected void onButton(int id) {
        if (id == BUTTON_SPIN) {
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
