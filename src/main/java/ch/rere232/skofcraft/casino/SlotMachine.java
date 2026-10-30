package ch.rere232.skofcraft.casino;

import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Supplier;

/** Reel symbols and payouts of the phone slot machine, shared by client display and server roll. */
public final class SlotMachine {
    public record Symbol(Supplier<? extends Item> item, int weight, int multiplier) {}

    public static final int[] BETS = {1, 2, 5};
    public static final int REELS = 3;

    public static final List<Symbol> SYMBOLS = List.of(
            new Symbol(() -> Items.APPLE, 30, 3),
            new Symbol(() -> Items.GOLD_INGOT, 25, 5),
            new Symbol(() -> Items.EMERALD, 20, 8),
            new Symbol(SkofcraftItems.CIGARETTE, 15, 12),
            new Symbol(() -> Items.DIAMOND, 10, 25)
    );

    public static final int JACKPOT_SYMBOL = SYMBOLS.size() - 1;

    private SlotMachine() {}

    public static int roll(RandomSource random) {
        int total = 0;
        for (Symbol symbol : SYMBOLS) {
            total += symbol.weight();
        }
        int pick = random.nextInt(total);
        for (int i = 0; i < SYMBOLS.size(); i++) {
            pick -= SYMBOLS.get(i).weight();
            if (pick < 0) {
                return i;
            }
        }
        return 0;
    }

    /** Three of a kind pays the symbol multiplier, any pair gives the bet back. */
    public static int payout(int[] reels, int bet) {
        if (reels[0] == reels[1] && reels[1] == reels[2]) {
            return bet * SYMBOLS.get(reels[0]).multiplier();
        }
        if (reels[0] == reels[1] || reels[1] == reels[2] || reels[0] == reels[2]) {
            return bet;
        }
        return 0;
    }
}
