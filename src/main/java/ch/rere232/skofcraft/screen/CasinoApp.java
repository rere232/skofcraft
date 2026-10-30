package ch.rere232.skofcraft.screen;

import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Casino app: slots, blackjack and roulette, all played for real diamonds. */
@OnlyIn(Dist.CLIENT)
public class CasinoApp implements PhoneApp {
    private static final int HEADER_H = 22;
    private static final int TAB_H = 14;
    private static final int GOLD = 0xFFF2C14E;
    private static final String[] TAB_KEYS = {"slots", "blackjack", "roulette"};

    /** The app currently on screen, so server answers can reach it. */
    private static CasinoApp active;
    static int selectedBet;
    private static int lastTab;

    private final Font font;
    private final Random random = new Random();
    private final SlotsGame slots;
    private final BlackjackPanel blackjack;
    private final RouletteGame roulette;
    private final CasinoGame[] games;
    private int tab;
    private Integer frozenDiamonds;
    private int tabX;
    private int tabY;
    private long lastNanos = System.nanoTime();

    public CasinoApp(Font font) {
        this.font = font;
        this.slots = new SlotsGame(this, font);
        this.blackjack = new BlackjackPanel(this, font);
        this.roulette = new RouletteGame(this, font);
        this.games = new CasinoGame[] {slots, blackjack, roulette};
        active = this;
        selectTab(lastTab);
    }

    public static void onResult(int[] reels, int bet, int payout) {
        if (active != null && active.slots.isWaiting()) {
            active.slots.receive(reels, bet, payout);
            return;
        }
        lateResult(bet, payout);
    }

    public static void onRoulette(int number, int bet, int payout) {
        if (active != null && active.roulette.isWaiting()) {
            active.roulette.receive(number, bet, payout);
            return;
        }
        lateResult(bet, payout);
    }

    public static void onBlackjack(int phase, int result, int bet, int payout, int[] player, int[] dealer) {
        if (active != null) {
            active.blackjack.receive(phase, result, bet, payout, player, dealer);
        }
    }

    private static void lateResult(int bet, int payout) {
        Minecraft.getInstance().gui.getChat().addMessage(
                Component.translatable("phone.skofcraft.casino.late", bet, payout).withStyle(ChatFormatting.GOLD));
    }

    private void selectTab(int index) {
        tab = index;
        lastTab = index;
        games[tab].onShow();
    }

    @Override
    public void onClose() {
        if (active == this) {
            active = null;
        }
    }

    @Override
    public void tick() {}

    Random random() {
        return random;
    }

    boolean creative() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.getAbilities().instabuild;
    }

    int diamonds() {
        LocalPlayer player = Minecraft.getInstance().player;
        int total = 0;
        if (player != null) {
            for (ItemStack stack : player.getInventory().items) {
                if (stack.is(Items.DIAMOND)) {
                    total += stack.getCount();
                }
            }
        }
        return total;
    }

    boolean canAfford(int amount) {
        return creative() || diamonds() >= amount;
    }

    /** Keeps the header count at its pre-bet value until the animation reveals the outcome. */
    void freezeDiamonds() {
        frozenDiamonds = diamonds();
    }

    void unfreezeDiamonds() {
        frozenDiamonds = null;
    }

    void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
    }

    @Override
    public void render(GuiGraphics g, Font font, int x, int y, int w, int h, int mouseX, int mouseY) {
        long now = System.nanoTime();
        float dt = Math.min(0.05F, (now - lastNanos) / 1_000_000_000.0F);
        lastNanos = now;

        g.fill(x, y, x + w, y + h, 0xFF14080C);
        int gy = y + HEADER_H + TAB_H;
        CasinoGame game = games[tab];
        game.beginFrame();
        game.render(g, x, gy, w, h - HEADER_H - TAB_H, dt);
        drawHeader(g, x, y, w);
        tabX = x;
        tabY = y + HEADER_H;
        drawTabs(g, x, tabY, w);
    }

    private void drawHeader(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + HEADER_H, 0xFF2A0D12);
        g.drawString(font, Component.translatable("phone.skofcraft.casino").withStyle(ChatFormatting.BOLD),
                x + 6, y + 4, GOLD, false);
        g.pose().pushPose();
        g.pose().translate(x + 6, y + 14, 0);
        g.pose().scale(0.7F, 0.7F, 1.0F);
        g.drawString(font, Component.translatable("phone.skofcraft.casino.tagline"), 0, 0, 0xFFB98A8F, false);
        g.pose().popPose();

        String count = creative() ? "inf" : Integer.toString(frozenDiamonds != null ? frozenDiamonds : diamonds());
        g.pose().pushPose();
        g.pose().translate(x + w - 14, y + 4, 0);
        g.pose().scale(0.5F, 0.5F, 1.0F);
        g.renderItem(new ItemStack(Items.DIAMOND), 0, 0);
        g.pose().popPose();
        g.pose().pushPose();
        g.pose().translate(x + w - 16 - font.width(count) * 0.8F, y + 5, 0);
        g.pose().scale(0.8F, 0.8F, 1.0F);
        g.drawString(font, count, 0, 0, MachineScreenHelper.COLOR_TEXT, false);
        g.pose().popPose();
    }

    private void drawTabs(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + TAB_H, 0xFF2A0D12);
        g.fill(x, y + TAB_H - 1, x + w, y + TAB_H, GOLD);
        int tabW = w / TAB_KEYS.length;
        for (int i = 0; i < TAB_KEYS.length; i++) {
            int tx = x + i * tabW;
            boolean selected = i == tab;
            if (selected) {
                g.fill(tx + 2, y + 1, tx + tabW - 2, y + TAB_H - 1, GOLD);
            }
            Component label = Component.translatable("phone.skofcraft.casino.tab." + TAB_KEYS[i]);
            g.pose().pushPose();
            g.pose().translate(tx + (tabW - font.width(label) * 0.75F) / 2.0F, y + 3.5F, 0);
            g.pose().scale(0.75F, 0.75F, 1.0F);
            g.drawString(font, label, 0, 0, selected ? 0xFF111111 : 0xFFD5D8DD, false);
            g.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        int tabTop = tabY;
        if (mouseY >= tabTop && mouseY < tabTop + TAB_H) {
            int tabW = PhoneScreen.SCREEN_W / TAB_KEYS.length;
            int index = Math.min(TAB_KEYS.length - 1, (int) ((mouseX - tabX) / tabW));
            if (index >= 0 && index != tab && !games[tab].isBusy()) {
                selectTab(index);
                play(SoundEvents.UI_BUTTON_CLICK.get(), 1.2F);
            }
            return true;
        }
        return games[tab].mouseClicked(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode) {
        return games[tab].keyPressed(keyCode);
    }
}
