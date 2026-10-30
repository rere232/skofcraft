package ch.rere232.skofcraft.casino;

import ch.rere232.skofcraft.delivery.DeliveryService;
import ch.rere232.skofcraft.network.BlackjackStateS2CPacket;
import ch.rere232.skofcraft.network.CasinoResultS2CPacket;
import ch.rere232.skofcraft.network.RouletteResultS2CPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server side of the phone casino: every bet is charged and paid here, never on the client. */
public final class CasinoService {
    private static final int COOLDOWN_TICKS = 30;
    private static final Map<UUID, Long> LAST_SPIN = new HashMap<>();
    private static final Map<UUID, BlackjackGame> TABLES = new HashMap<>();

    private CasinoService() {}

    private static boolean hasPhone(ServerPlayer player) {
        return player.isAlive() && player.getInventory().hasAnyMatching(stack -> stack.is(SkofcraftItems.PHONE.get()));
    }

    private static boolean cooledDown(ServerPlayer player) {
        long now = player.level().getGameTime();
        Long last = LAST_SPIN.get(player.getUUID());
        if (last != null && now - last < COOLDOWN_TICKS) {
            return false;
        }
        LAST_SPIN.put(player.getUUID(), now);
        return true;
    }

    /** Charges the bet; returns false (with a message) if the player cannot pay. */
    private static boolean charge(ServerPlayer player, int amount) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        if (DeliveryService.countDiamonds(player.getInventory()) < amount) {
            player.displayClientMessage(Component.translatable("phone.skofcraft.amazooon.no_funds"), true);
            return false;
        }
        DeliveryService.removeDiamonds(player.getInventory(), amount);
        return true;
    }

    public static void spin(ServerPlayer player, int betIndex) {
        if (betIndex < 0 || betIndex >= SlotMachine.BETS.length || !hasPhone(player)) {
            return;
        }
        int bet = SlotMachine.BETS[betIndex];
        if (!cooledDown(player) || !charge(player, bet)) {
            return;
        }

        int[] reels = new int[SlotMachine.REELS];
        for (int i = 0; i < reels.length; i++) {
            reels[i] = SlotMachine.roll(player.getRandom());
        }
        int payout = SlotMachine.payout(reels, bet);
        giveDiamonds(player, payout);
        SkofcraftNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new CasinoResultS2CPacket(reels, bet, payout));
    }

    public static void roulette(ServerPlayer player, int betIndex, int type, int pick) {
        if (betIndex < 0 || betIndex >= SlotMachine.BETS.length || type < 0 || type >= Roulette.TYPE_COUNT
                || pick < 0 || pick > 36 || !hasPhone(player)) {
            return;
        }
        int bet = SlotMachine.BETS[betIndex];
        if (!cooledDown(player) || !charge(player, bet)) {
            return;
        }

        int number = player.getRandom().nextInt(37);
        int payout = Roulette.payout(type, pick, number, bet);
        giveDiamonds(player, payout);
        SkofcraftNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new RouletteResultS2CPacket(number, bet, payout));
    }

    public static void blackjack(ServerPlayer player, int action, int betIndex) {
        BlackjackGame game = TABLES.get(player.getUUID());
        if (action == BlackjackGame.ACTION_SYNC) {
            sendTable(player, game);
            return;
        }
        if (!hasPhone(player)) {
            return;
        }

        if (action == BlackjackGame.ACTION_DEAL) {
            if (game != null && game.phase() == BlackjackGame.PLAYING) {
                sendTable(player, game);
                return;
            }
            if (betIndex < 0 || betIndex >= SlotMachine.BETS.length) {
                return;
            }
            int bet = SlotMachine.BETS[betIndex];
            if (!cooledDown(player) || !charge(player, bet)) {
                sendTable(player, game);
                return;
            }
            game = new BlackjackGame(bet, player.getRandom());
            TABLES.put(player.getUUID(), game);
        } else if (game == null || game.phase() != BlackjackGame.PLAYING) {
            sendTable(player, game);
            return;
        } else if (action == BlackjackGame.ACTION_HIT) {
            game.hit();
        } else if (action == BlackjackGame.ACTION_STAND) {
            game.stand();
        } else if (action == BlackjackGame.ACTION_DOUBLE) {
            if (game.canDouble() && charge(player, game.bet())) {
                game.doubleDown();
            }
        }

        if (game.phase() == BlackjackGame.DONE && !game.isPaid()) {
            game.markPaid();
            giveDiamonds(player, game.payout());
        }
        sendTable(player, game);
    }

    private static void sendTable(ServerPlayer player, BlackjackGame game) {
        BlackjackStateS2CPacket packet = game == null
                ? new BlackjackStateS2CPacket(0, 0, 0, 0, new int[0], new int[0])
                : new BlackjackStateS2CPacket(game.phase(), game.result(), game.bet(), game.payout(),
                        game.playerCards(), game.dealerCardsForClient());
        SkofcraftNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private static void giveDiamonds(ServerPlayer player, int amount) {
        while (amount > 0) {
            int count = Math.min(amount, 64);
            ItemStack stack = new ItemStack(Items.DIAMOND, count);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            amount -= count;
        }
    }
}
