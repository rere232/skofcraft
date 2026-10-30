package ch.rere232.skofcraft.casino;

/** European single-zero roulette. */
public final class Roulette {
    public static final int TYPE_RED = 0;
    public static final int TYPE_BLACK = 1;
    public static final int TYPE_ODD = 2;
    public static final int TYPE_EVEN = 3;
    public static final int TYPE_LOW = 4;
    public static final int TYPE_HIGH = 5;
    public static final int TYPE_NUMBER = 6;
    public static final int TYPE_COUNT = 7;

    /** Pocket order around the wheel. */
    public static final int[] WHEEL = {0, 32, 15, 19, 4, 21, 2, 25, 17, 34, 6, 27, 13, 36, 11, 30, 8, 23, 10, 5, 24,
            16, 33, 1, 20, 14, 31, 9, 22, 18, 29, 7, 28, 12, 35, 3, 26};

    private static final boolean[] RED = new boolean[37];

    static {
        for (int n : new int[] {1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36}) {
            RED[n] = true;
        }
    }

    private Roulette() {}

    public static boolean isRed(int number) {
        return RED[number];
    }

    /** Total diamonds returned: 36x for a straight-up number, 2x for the even-money bets. */
    public static int payout(int type, int pick, int number, int bet) {
        if (type == TYPE_NUMBER) {
            return number == pick ? bet * 36 : 0;
        }
        if (number == 0) {
            return 0;
        }
        boolean win = switch (type) {
            case TYPE_RED -> isRed(number);
            case TYPE_BLACK -> !isRed(number);
            case TYPE_ODD -> number % 2 == 1;
            case TYPE_EVEN -> number % 2 == 0;
            case TYPE_LOW -> number <= 18;
            case TYPE_HIGH -> number >= 19;
            default -> false;
        };
        return win ? bet * 2 : 0;
    }
}
