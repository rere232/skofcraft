package ch.rere232.skofcraft.casino;

import java.util.List;

/** Playing cards are ints 0..51: rank = card % 13 (0 = ace .. 12 = king), suit = card / 13. */
public final class Cards {
    private Cards() {}

    public static int rank(int card) {
        return card % 13;
    }

    public static int suit(int card) {
        return card / 13;
    }

    private static int value(int card) {
        int rank = rank(card);
        if (rank == 0) {
            return 11;
        }
        return Math.min(rank + 1, 10);
    }

    /** Best blackjack total of a hand; negative ids (hidden cards) are ignored. */
    public static int total(Iterable<Integer> hand) {
        int sum = 0;
        int aces = 0;
        for (int card : hand) {
            if (card < 0) {
                continue;
            }
            sum += value(card);
            if (rank(card) == 0) {
                aces++;
            }
        }
        while (sum > 21 && aces > 0) {
            sum -= 10;
            aces--;
        }
        return sum;
    }

    public static int total(int[] hand) {
        return total(java.util.Arrays.stream(hand).boxed().toList());
    }

    public static int[] toArray(List<Integer> hand) {
        return hand.stream().mapToInt(Integer::intValue).toArray();
    }
}
