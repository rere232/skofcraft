package ch.rere232.skofcraft.casino;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.util.RandomSource;

/** One server-side blackjack round: dealer stands on 17, blackjack pays 3:2 (rounded down), double down allowed. */
public final class BlackjackGame {
    public static final int ACTION_SYNC = 0;
    public static final int ACTION_DEAL = 1;
    public static final int ACTION_HIT = 2;
    public static final int ACTION_STAND = 3;
    public static final int ACTION_DOUBLE = 4;

    public static final int PLAYING = 1;
    public static final int DONE = 2;

    public static final int WIN = 1;
    public static final int PUSH = 2;
    public static final int LOSE = 3;
    public static final int BLACKJACK = 4;
    public static final int BUST = 5;

    private final List<Integer> deck = new ArrayList<>();
    private final List<Integer> player = new ArrayList<>();
    private final List<Integer> dealer = new ArrayList<>();
    private int bet;
    private int phase = PLAYING;
    private int result;
    private int payout;
    private boolean doubled;
    private boolean paid;

    public BlackjackGame(int bet, RandomSource random) {
        this.bet = bet;
        for (int i = 0; i < 52; i++) {
            deck.add(i);
        }
        Collections.shuffle(deck, new Random(random.nextLong()));
        player.add(draw());
        dealer.add(draw());
        player.add(draw());
        dealer.add(draw());

        boolean playerNatural = Cards.total(player) == 21;
        boolean dealerNatural = Cards.total(dealer) == 21;
        if (playerNatural && dealerNatural) {
            finish(PUSH, bet);
        } else if (playerNatural) {
            finish(BLACKJACK, bet + bet * 3 / 2);
        } else if (dealerNatural) {
            finish(LOSE, 0);
        }
    }

    private int draw() {
        return deck.remove(deck.size() - 1);
    }

    private void finish(int result, int payout) {
        this.phase = DONE;
        this.result = result;
        this.payout = payout;
    }

    public void hit() {
        player.add(draw());
        int total = Cards.total(player);
        if (total > 21) {
            finish(BUST, 0);
        } else if (total == 21) {
            stand();
        }
    }

    public void stand() {
        while (Cards.total(dealer) < 17) {
            dealer.add(draw());
        }
        int p = Cards.total(player);
        int d = Cards.total(dealer);
        if (d > 21 || p > d) {
            finish(WIN, bet * 2);
        } else if (p == d) {
            finish(PUSH, bet);
        } else {
            finish(LOSE, 0);
        }
    }

    public boolean canDouble() {
        return phase == PLAYING && player.size() == 2 && !doubled;
    }

    /** The caller must already have charged the extra stake of {@link #bet()}. */
    public void doubleDown() {
        doubled = true;
        bet *= 2;
        player.add(draw());
        if (Cards.total(player) > 21) {
            finish(BUST, 0);
        } else {
            stand();
        }
    }

    public int bet() {
        return bet;
    }

    public int phase() {
        return phase;
    }

    public int result() {
        return result;
    }

    public int payout() {
        return payout;
    }

    public boolean isPaid() {
        return paid;
    }

    public void markPaid() {
        paid = true;
    }

    public int[] playerCards() {
        return Cards.toArray(player);
    }

    /** While the round is running the hole card is replaced by -1. */
    public int[] dealerCardsForClient() {
        int[] cards = Cards.toArray(dealer);
        if (phase == PLAYING) {
            for (int i = 1; i < cards.length; i++) {
                cards[i] = -1;
            }
        }
        return cards;
    }
}
