package ch.rere232.skofcraft.screen;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.casino.BlackjackGame;
import ch.rere232.skofcraft.casino.Cards;
import ch.rere232.skofcraft.casino.SlotMachine;
import ch.rere232.skofcraft.network.BlackjackActionC2SPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Blackjack table; the server owns the deck and the dealer, this class only shows the state it sends. */
@OnlyIn(Dist.CLIENT)
final class BlackjackPanel extends CasinoGame {
    private static final int BUTTON_HIT = 1;
    private static final int BUTTON_STAND = 2;
    private static final int BUTTON_DOUBLE = 3;
    private static final int BUTTON_DEAL = 4;
    private static final int CARD_W = 22;
    private static final int CARD_H = 30;
    private static final int CARD_STEP = 14;
    private static final float TIMEOUT = 3.0F;
    private static final String[] RANKS = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
    /** Spade, heart, diamond, club as 5x5 bitmaps. */
    private static final String[][] SUITS = {
            {"..X..", ".XXX.", "XXXXX", "XXXXX", "..X.."},
            {".X.X.", "XXXXX", "XXXXX", ".XXX.", "..X.."},
            {"..X..", ".XXX.", "XXXXX", ".XXX.", "..X.."},
            {".XXX.", ".XXX.", "XXXXX", "XXXXX", "..X.."}
    };

    private int phase;
    private int result;
    private int bet;
    private int payout;
    private int[] player = new int[0];
    private int[] dealer = new int[0];
    private boolean waiting;
    private float waitTime;
    private float flash;

    BlackjackPanel(CasinoApp host, Font font) {
        super(host, font);
    }

    @Override
    boolean isBusy() {
        return waiting;
    }

    @Override
    void onShow() {
        send(BlackjackGame.ACTION_SYNC);
    }

    private void send(int action) {
        waiting = true;
        waitTime = 0.0F;
        SkofcraftNetwork.CHANNEL.sendToServer(new BlackjackActionC2SPacket(action, CasinoApp.selectedBet));
    }

    void receive(int phase, int result, int bet, int payout, int[] player, int[] dealer) {
        boolean finished = phase == BlackjackGame.DONE && this.phase != BlackjackGame.DONE;
        this.phase = phase;
        this.result = result;
        this.bet = bet;
        this.payout = payout;
        this.player = player;
        this.dealer = dealer;
        waiting = false;
        if (finished && (result == BlackjackGame.WIN || result == BlackjackGame.BLACKJACK)) {
            host.play(SkofcraftSounds.COIN.get(), 1.0F);
        }
    }

    @Override
    void render(GuiGraphics g, int x, int y, int w, int h, float dt) {
        flash += dt;
        if (waiting) {
            waitTime += dt;
            if (waitTime > TIMEOUT) {
                waiting = false;
            }
        }

        boolean playing = phase == BlackjackGame.PLAYING;
        String dealerTotal = phase == BlackjackGame.DONE || dealer.length == 0
                ? Integer.toString(Cards.total(dealer))
                : Cards.total(dealer) + "+?";
        drawSmall(g, Component.translatable("phone.skofcraft.casino.bj.dealer").getString(), x + 6, y + 1, DIM);
        drawSmall(g, dealerTotal, x + w - 6 - (int) (font.width(dealerTotal) * 0.7F), y + 1, 0xFFD5D8DD);
        drawHand(g, x + 6, y + 9, dealer);

        drawSmall(g, Component.translatable("phone.skofcraft.casino.bj.you").getString(), x + 6, y + 44, DIM);
        String playerTotal = Integer.toString(Cards.total(player));
        drawSmall(g, playerTotal, x + w - 6 - (int) (font.width(playerTotal) * 0.7F), y + 44, 0xFFD5D8DD);
        drawHand(g, x + 6, y + 53, player);

        drawResultText(g, x + w / 2, y + 90);

        if (playing) {
            int bw = (w - 12 - 8) / 3;
            boolean canDouble = player.length == 2 && host.canAfford(bet);
            button(g, BUTTON_HIT, x + 6, y + 106, bw, 16, Component.translatable("phone.skofcraft.casino.bj.hit"),
                    0xFF2E7D4F, 0xFFFFFFFF, !waiting, false);
            button(g, BUTTON_STAND, x + 6 + bw + 4, y + 106, bw, 16,
                    Component.translatable("phone.skofcraft.casino.bj.stand"), RED, 0xFFFFFFFF, !waiting, false);
            button(g, BUTTON_DOUBLE, x + 6 + (bw + 4) * 2, y + 106, bw, 16,
                    Component.translatable("phone.skofcraft.casino.bj.double"), 0xFF3A5FA0, 0xFFFFFFFF,
                    !waiting && canDouble, false);
        } else {
            betRow(g, x, y + 104, w, !waiting);
            boolean enabled = !waiting && host.canAfford(SlotMachine.BETS[CasinoApp.selectedBet]);
            int sw = 80;
            int sx = x + (w - sw) / 2;
            int sy = y + 126;
            MachineScreenHelper.fillRounded(g, sx, sy + 1, sx + sw, sy + 18, enabled ? 0xFF7A1521 : 0xFF3A404C);
            button(g, BUTTON_DEAL, sx, sy, sw, 17, Component.translatable("phone.skofcraft.casino.bj.deal")
                    .withStyle(ChatFormatting.BOLD), enabled ? RED : 0xFF2A2F38, 0xFFFFFFFF, enabled, false);
        }
        drawSmall(g, Component.translatable("phone.skofcraft.casino.bj.rules").getString(), x + 6, y + 146, DIM);
    }

    private void drawResultText(GuiGraphics g, int cx, int y) {
        Component text;
        int color;
        if (phase == 0) {
            text = Component.translatable("phone.skofcraft.casino.place");
            color = DIM;
        } else if (phase == BlackjackGame.PLAYING) {
            text = Component.translatable("phone.skofcraft.casino.bj.turn");
            color = 0xFFD5D8DD;
        } else if (result == BlackjackGame.BLACKJACK) {
            text = Component.translatable("phone.skofcraft.casino.bj.blackjack", payout);
            color = (int) (flash * 8) % 2 == 0 ? 0xFFFFFFFF : GOLD;
        } else if (result == BlackjackGame.WIN) {
            text = Component.translatable("phone.skofcraft.casino.win", payout);
            color = 0xFF3DDC97;
        } else if (result == BlackjackGame.PUSH) {
            text = Component.translatable("phone.skofcraft.casino.push");
            color = 0xFFD5D8DD;
        } else if (result == BlackjackGame.BUST) {
            text = Component.translatable("phone.skofcraft.casino.bj.bust");
            color = 0xFFE5484D;
        } else {
            text = Component.translatable("phone.skofcraft.casino.bj.lose");
            color = 0xFFE5484D;
        }
        drawCentered(g, text, cx, y, 0.85F, color);
    }

    private void drawHand(GuiGraphics g, int x, int y, int[] cards) {
        for (int i = 0; i < cards.length; i++) {
            drawCard(g, x + i * CARD_STEP, y, cards[i]);
        }
    }

    private void drawCard(GuiGraphics g, int x, int y, int card) {
        MachineScreenHelper.fillRounded(g, x, y, x + CARD_W, y + CARD_H, 0xFF05060A);
        if (card < 0) {
            MachineScreenHelper.fillRounded(g, x + 1, y + 1, x + CARD_W - 1, y + CARD_H - 1, 0xFF1E3A8A);
            for (int row = 0; row < 6; row++) {
                g.fill(x + 3, y + 3 + row * 4, x + CARD_W - 3, y + 4 + row * 4, 0xFF4A66B0);
            }
            return;
        }
        MachineScreenHelper.fillRounded(g, x + 1, y + 1, x + CARD_W - 1, y + CARD_H - 1, 0xFFF4F0E6);
        int suit = Cards.suit(card);
        int color = suit == 1 || suit == 2 ? 0xFFC8283A : 0xFF111111;
        String rank = RANKS[Cards.rank(card)];
        g.pose().pushPose();
        g.pose().translate(x + 3, y + 3, 0);
        g.pose().scale(0.8F, 0.8F, 1.0F);
        g.drawString(font, rank, 0, 0, color, false);
        g.pose().popPose();
        String[] bitmap = SUITS[suit];
        for (int row = 0; row < bitmap.length; row++) {
            for (int col = 0; col < bitmap[row].length(); col++) {
                if (bitmap[row].charAt(col) == 'X') {
                    int px = x + 6 + col * 2;
                    int py = y + 14 + row * 3 - 1;
                    g.fill(px, py, px + 2, py + 3, color);
                }
            }
        }
    }

    @Override
    protected void onButton(int id) {
        if (waiting) {
            return;
        }
        host.play(SkofcraftSounds.UI_CLICK.get(), 1.0F);
        switch (id) {
            case BUTTON_HIT -> send(BlackjackGame.ACTION_HIT);
            case BUTTON_STAND -> send(BlackjackGame.ACTION_STAND);
            case BUTTON_DOUBLE -> send(BlackjackGame.ACTION_DOUBLE);
            case BUTTON_DEAL -> send(BlackjackGame.ACTION_DEAL);
            default -> { }
        }
    }

    @Override
    boolean keyPressed(int keyCode) {
        if (keyCode == 32 && phase != BlackjackGame.PLAYING && !waiting && host.canAfford(SlotMachine.BETS[CasinoApp.selectedBet])) {
            send(BlackjackGame.ACTION_DEAL);
            return true;
        }
        return false;
    }
}
