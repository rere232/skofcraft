package ch.rere232.skofcraft.screen;

import ch.rere232.skofcraft.delivery.AmazooonCatalog;
import ch.rere232.skofcraft.network.AmazooonOrderC2SPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Delivery shop app: pay diamonds and a plane drops the order on your head. */
@OnlyIn(Dist.CLIENT)
public class AmazooonApp implements PhoneApp {
    private static final int HEADER_H = 22;
    private static final int ROW_H = 30;
    private static final int ROW_GAP = 3;
    private static final int BUTTON_W = 30;
    private static final int BUTTON_H = 12;
    private static final int TOAST_TICKS = 100;
    private static final int NAVY = 0xFF131A22;
    private static final int ORANGE = 0xFFFF9900;

    private final Font font;
    /** Buy button hit boxes of the current frame: product index, x, y, width, height. */
    private final List<int[]> buyBoxes = new ArrayList<>();
    private float scroll;
    private int listTop;
    private int listBottom;
    private int toastTicks;

    public AmazooonApp(Font font) {
        this.font = font;
    }

    @Override
    public void tick() {
        if (toastTicks > 0) {
            toastTicks--;
        }
    }

    private int contentHeight() {
        return AmazooonCatalog.PRODUCTS.size() * (ROW_H + ROW_GAP) + ROW_GAP;
    }

    private float maxScroll() {
        return Math.max(0.0F, contentHeight() - (listBottom - listTop));
    }

    private int diamonds() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 0;
        }
        Inventory inventory = player.getInventory();
        int total = 0;
        for (ItemStack stack : inventory.items) {
            if (stack.is(Items.DIAMOND)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private boolean creative() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.getAbilities().instabuild;
    }

    @Override
    public void render(GuiGraphics g, Font font, int x, int y, int w, int h, int mouseX, int mouseY) {
        listTop = y + HEADER_H;
        listBottom = y + h;
        scroll = Math.min(scroll, maxScroll());
        buyBoxes.clear();

        g.fill(x, y, x + w, y + h, 0xFF0D1218);
        int have = diamonds();
        boolean free = creative();

        g.enableScissor(x, listTop, x + w, listBottom);
        for (int i = 0; i < AmazooonCatalog.PRODUCTS.size(); i++) {
            int rowY = listTop + ROW_GAP + i * (ROW_H + ROW_GAP) - (int) scroll;
            if (rowY + ROW_H < listTop || rowY > listBottom) {
                continue;
            }
            drawRow(g, x + 4, rowY, w - 8, i, free || have >= AmazooonCatalog.PRODUCTS.get(i).price());
        }
        g.disableScissor();

        drawHeader(g, x, y, w, have);
        drawToast(g, x, y + h, w);
    }

    private void drawHeader(GuiGraphics g, int x, int y, int w, int have) {
        g.fill(x, y, x + w, y + HEADER_H, NAVY);
        g.fill(x, y + HEADER_H - 1, x + w, y + HEADER_H, 0xFF2B3544);
        g.drawString(font, Component.translatable("phone.skofcraft.amazooon").withStyle(ChatFormatting.BOLD),
                x + 6, y + 4, 0xFFFFFFFF, false);
        // smile arrow underneath the logo
        int logoW = font.width(Component.translatable("phone.skofcraft.amazooon").withStyle(ChatFormatting.BOLD));
        g.fill(x + 8, y + 14, x + 8 + logoW - 4, y + 15, ORANGE);
        g.fill(x + 6, y + 13, x + 8, y + 14, ORANGE);
        g.fill(x + 4 + logoW, y + 13, x + 6 + logoW, y + 14, ORANGE);
        g.fill(x + 3 + logoW, y + 12, x + 5 + logoW, y + 13, ORANGE);

        g.pose().pushPose();
        g.pose().translate(x + w - 6 - 8, y + 4, 0);
        g.pose().scale(0.5F, 0.5F, 1.0F);
        g.renderItem(new ItemStack(Items.DIAMOND), 0, 0);
        g.pose().popPose();
        String count = creative() ? "inf" : Integer.toString(have);
        g.pose().pushPose();
        g.pose().translate(x + w - 6 - 8 - 2 - font.width(count) * 0.8F, y + 5, 0);
        g.pose().scale(0.8F, 0.8F, 1.0F);
        g.drawString(font, count, 0, 0, MachineScreenHelper.COLOR_TEXT, false);
        g.pose().popPose();
        drawSmall(g, Component.translatable("phone.skofcraft.amazooon.tagline").getString(), x + 6, y + 15,
                MachineScreenHelper.COLOR_TEXT_DIM);
    }

    private void drawRow(GuiGraphics g, int x, int y, int w, int index, boolean affordable) {
        AmazooonCatalog.Product product = AmazooonCatalog.PRODUCTS.get(index);
        MachineScreenHelper.fillRounded(g, x, y, x + w, y + ROW_H, 0xFF1B222C);
        MachineScreenHelper.fillRounded(g, x + 3, y + 7, x + 3 + 18, y + 7 + 16, 0xFF0D1218);
        ItemStack stack = product.stack();
        g.renderItem(stack, x + 4, y + 7);

        int textX = x + 25;
        int maxName = w - 25 - BUTTON_W - 8;
        String name = font.plainSubstrByWidth(stack.getHoverName().getString(), (int) (maxName / 0.8F));
        g.pose().pushPose();
        g.pose().translate(textX, y + 5, 0);
        g.pose().scale(0.8F, 0.8F, 1.0F);
        g.drawString(font, name, 0, 0, MachineScreenHelper.COLOR_TEXT, false);
        g.pose().popPose();
        drawSmall(g, "x" + product.count(), textX, y + 15, MachineScreenHelper.COLOR_TEXT_DIM);

        g.pose().pushPose();
        g.pose().translate(textX, y + 20, 0);
        g.pose().scale(0.5F, 0.5F, 1.0F);
        g.renderItem(new ItemStack(Items.DIAMOND), 0, 0);
        g.pose().popPose();
        drawSmall(g, Integer.toString(product.price()), textX + 10, y + 21, 0xFF6FD6E8);

        int bx = x + w - BUTTON_W - 4;
        int by = y + (ROW_H - BUTTON_H) / 2;
        MachineScreenHelper.fillRounded(g, bx, by, bx + BUTTON_W, by + BUTTON_H, affordable ? ORANGE : 0xFF3A404C);
        Component label = Component.translatable("phone.skofcraft.amazooon.buy");
        g.pose().pushPose();
        g.pose().translate(bx + (BUTTON_W - font.width(label) * 0.8F) / 2.0F, by + 3, 0);
        g.pose().scale(0.8F, 0.8F, 1.0F);
        g.drawString(font, label, 0, 0, affordable ? 0xFF111111 : MachineScreenHelper.COLOR_TEXT_DIM, false);
        g.pose().popPose();
        buyBoxes.add(new int[] {index, bx, by, BUTTON_W, BUTTON_H});
    }

    private void drawToast(GuiGraphics g, int x, int bottom, int w) {
        if (toastTicks <= 0) {
            return;
        }
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        MachineScreenHelper.fillRounded(g, x + 6, bottom - 18, x + w - 6, bottom - 4, 0xEE05080C);
        Component text = Component.translatable("phone.skofcraft.amazooon.ordered");
        g.pose().pushPose();
        g.pose().translate(x + (w - font.width(text) * 0.8F) / 2.0F, bottom - 14, 0);
        g.pose().scale(0.8F, 0.8F, 1.0F);
        g.drawString(font, text, 0, 0, ORANGE, false);
        g.pose().popPose();
        g.pose().popPose();
    }

    private void drawSmall(GuiGraphics g, String text, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.7F, 0.7F, 1.0F);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || mouseY < listTop || mouseY >= listBottom) {
            return false;
        }
        for (int[] box : buyBoxes) {
            if (mouseX >= box[1] && mouseX < box[1] + box[3] && mouseY >= box[2] && mouseY < box[2] + box[4]) {
                AmazooonCatalog.Product product = AmazooonCatalog.PRODUCTS.get(box[0]);
                if (creative() || diamonds() >= product.price()) {
                    SkofcraftNetwork.CHANNEL.sendToServer(new AmazooonOrderC2SPacket(box[0]));
                    toastTicks = TOAST_TICKS;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double dragY) {
        scroll = Math.max(0.0F, Math.min(maxScroll(), scroll - (float) dragY));
        return true;
    }

    @Override
    public boolean mouseScrolled(double delta) {
        scroll = Math.max(0.0F, Math.min(maxScroll(), scroll - (float) delta * 14.0F));
        return true;
    }
}
