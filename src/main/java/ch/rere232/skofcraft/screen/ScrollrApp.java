package ch.rere232.skofcraft.screen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import ch.rere232.skofcraft.client.PhoneClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Social feed app: an endless, randomly generated feed to doomscroll. */
@OnlyIn(Dist.CLIENT)
public class ScrollrApp implements PhoneApp {
    private static final int HEADER_H = 18;
    private static final int POST_COUNT = 40;
    private static final int AD_COUNT = 5;
    private static final float TEXT_SCALE = 0.8F;
    private static final int LINE_H = 7;
    private static final int IMAGE_H = 40;
    /** From this phone addiction the feed scrolls by itself. */
    private static final int AUTOPLAY_LEVEL = 60;

    private static final String[] USERS = {
            "xX_CreeperSlayer_Xx", "notch_fan_2009", "steve_but_tired", "herobrine_not_here", "villager_hmm",
            "golem_dad", "enderman_stare", "bee_movie_guy", "sleepy_steve", "ghast_tears", "diamond_dave",
            "redstone_rita", "lava_larry", "skeleton_aim", "zombie_nap", "cow_enjoyer"
    };

    private record Post(int index, String user, boolean ad, List<FormattedCharSequence> lines,
                        int imageSeed, int likes, int replies, int minutesAgo, int height) {
        boolean hasImage() {
            return imageSeed != 0;
        }
    }

    private final Font font;
    private final List<Post> posts = new ArrayList<>();
    private final List<Integer> tops = new ArrayList<>();
    private final Set<Integer> liked = new HashSet<>();
    /** Heart hit boxes of the current frame: post index, x, y, width, height. */
    private final List<int[]> heartBoxes = new ArrayList<>();
    private int totalHeight;
    private float scroll;
    private float velocity;
    private double lastDragY;
    private int screenTicks;
    private int shownMessages;

    private int areaW = PhoneScreen.SCREEN_W;
    private int feedTop;
    private int feedBottom;

    public ScrollrApp(Font font) {
        this.font = font;
    }

    @Override
    public void tick() {
        screenTicks++;
        int addiction = PhoneClient.addiction();
        if (addiction >= AUTOPLAY_LEVEL) {
            scroll += 0.3F + (addiction - AUTOPLAY_LEVEL) * 0.02F;
        }
        scroll = Math.max(0.0F, scroll + velocity);
        if (scroll <= 0.0F) {
            velocity = 0.0F;
        }
        velocity *= 0.92F;
        if (Math.abs(velocity) < 0.05F) {
            velocity = 0.0F;
        }
        announceMilestones();
    }

    private void announceMilestones() {
        int seen = seenPosts();
        int[] thresholds = {30, 100, 250};
        if (shownMessages < thresholds.length && seen >= thresholds[shownMessages]) {
            Minecraft.getInstance().gui.getChat().addMessage(
                    Component.translatable("phone.skofcraft.milestone." + shownMessages).withStyle(ChatFormatting.GRAY));
            shownMessages++;
        }
    }

    private int seenPosts() {
        float limit = scroll + (feedBottom - feedTop);
        int count = 0;
        for (int i = 0; i < tops.size() && tops.get(i) < limit; i++) {
            count = i + 1;
        }
        return count;
    }

    @Override
    public void render(GuiGraphics g, Font font, int x, int y, int w, int h, int mouseX, int mouseY) {
        areaW = w;
        feedTop = y + HEADER_H;
        feedBottom = y + h;
        heartBoxes.clear();
        drawFeed(g, x, w);
        drawHeader(g, x, y, w);
    }

    private void drawHeader(GuiGraphics g, int sx, int top, int sw) {
        g.fill(sx, top, sx + sw, top + HEADER_H, 0xFF171A21);
        g.drawString(this.font, Component.translatable("phone.skofcraft.app").withStyle(ChatFormatting.BOLD),
                sx + 6, top + 2, MachineScreenHelper.COLOR_ACCENT, false);
        g.pose().pushPose();
        g.pose().translate(sx + 6, top + 11, 0);
        g.pose().scale(0.7F, 0.7F, 1.0F);
        g.drawString(this.font, Component.translatable("phone.skofcraft.tab_foryou"), 0, 0, MachineScreenHelper.COLOR_TEXT, false);
        g.drawString(this.font, Component.translatable("phone.skofcraft.tab_following"), 60, 0, MachineScreenHelper.COLOR_TEXT_DIM, false);
        g.pose().popPose();
        g.fill(sx + 6, top + HEADER_H - 2, sx + 6 + 36, top + HEADER_H - 1, MachineScreenHelper.COLOR_ACCENT);

        String screenTime = formatScreenTime();
        g.pose().pushPose();
        g.pose().translate(sx + sw - 6 - this.font.width(screenTime) * 0.7F, top + 4, 0);
        g.pose().scale(0.7F, 0.7F, 1.0F);
        g.drawString(this.font, screenTime, 0, 0, MachineScreenHelper.COLOR_TEXT_DIM, false);
        g.pose().popPose();
    }

    private String formatScreenTime() {
        int seconds = screenTicks / 20;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private void drawFeed(GuiGraphics g, int sx, int sw) {
        ensurePosts((int) scroll + (feedBottom - feedTop) + 200);

        g.enableScissor(sx, feedTop, sx + sw, feedBottom);
        for (int i = 0; i < posts.size(); i++) {
            int y = feedTop + tops.get(i) - (int) scroll;
            Post post = posts.get(i);
            if (y + post.height() < feedTop) {
                continue;
            }
            if (y > feedBottom) {
                break;
            }
            drawPost(g, post, sx, y, sw);
        }
        g.disableScissor();
    }

    private void ensurePosts(int untilY) {
        while (totalHeight < untilY) {
            Post post = makePost(posts.size());
            posts.add(post);
            tops.add(totalHeight);
            totalHeight += post.height();
        }
    }

    private Post makePost(int index) {
        Random r = new Random(index * 7919L + 104729L);
        boolean ad = index > 0 && index % 7 == 6;
        String user = ad ? "Sponsored" : USERS[r.nextInt(USERS.length)];
        Component text = Component.translatable(ad
                ? "phone.skofcraft.ad." + r.nextInt(AD_COUNT)
                : "phone.skofcraft.post." + r.nextInt(POST_COUNT));
        int imageSeed = r.nextInt(100) < 34 ? (r.nextInt() | 1) : 0;
        int likes = (int) Math.pow(10.0D, 1.0D + r.nextDouble() * 3.8D);
        int replies = likes / (4 + r.nextInt(12));
        int minutes = 1 + r.nextInt(59);

        int textWidth = (int) ((areaW - 24) / TEXT_SCALE);
        List<FormattedCharSequence> lines = this.font.split(text, textWidth);
        int height = 4 + 13 + lines.size() * LINE_H + 3 + (imageSeed != 0 ? IMAGE_H + 3 : 0) + 12 + 4 + 1;
        return new Post(index, user, ad, lines, imageSeed, likes, replies, minutes, height);
    }

    private void drawPost(GuiGraphics g, Post post, int sx, int y, int sw) {
        int textX = sx + 20;

        int avatar = post.ad() ? 0xFFE0A030 : 0xFF000000 | Mth.hsvToRgb((post.user().hashCode() & 0xFF) / 255.0F, 0.5F, 0.75F);
        MachineScreenHelper.fillRounded(g, sx + 4, y + 4, sx + 16, y + 16, avatar);
        String initial = post.user().substring(0, 1).toUpperCase();
        g.pose().pushPose();
        g.pose().translate(sx + 10 - this.font.width(initial) * 0.4F, y + 7, 0);
        g.pose().scale(0.8F, 0.8F, 1.0F);
        g.drawString(this.font, initial, 0, 0, 0xFFFFFFFF, false);
        g.pose().popPose();

        g.pose().pushPose();
        g.pose().translate(textX, y + 5, 0);
        g.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
        g.drawString(this.font, Component.literal(post.user()).withStyle(ChatFormatting.BOLD), 0, 0,
                post.ad() ? MachineScreenHelper.COLOR_ACCENT : MachineScreenHelper.COLOR_TEXT, false);
        g.drawString(this.font, post.minutesAgo() + "m", this.font.width(post.user()) + 6, 0, MachineScreenHelper.COLOR_TEXT_DIM, false);
        g.pose().popPose();

        int cursor = y + 4 + 13;
        g.pose().pushPose();
        g.pose().translate(textX, cursor, 0);
        g.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
        for (int i = 0; i < post.lines().size(); i++) {
            g.drawString(this.font, post.lines().get(i), 0, i * 9, 0xFFD5D8DD, false);
        }
        g.pose().popPose();
        cursor += post.lines().size() * LINE_H + 3;

        if (post.hasImage()) {
            drawImage(g, textX, cursor, sw - 24, IMAGE_H, post.imageSeed());
            cursor += IMAGE_H + 3;
        }

        drawStats(g, post, textX, cursor);
        g.fill(sx + 4, y + post.height() - 1, sx + sw - 4, y + post.height(), 0xFF23272F);
    }

    private void drawStats(GuiGraphics g, Post post, int x, int y) {
        boolean isLiked = liked.contains(post.index());
        int heartColor = isLiked ? 0xFFE5484D : 0xFF6E7480;
        String[] heart = {".XX.XX.", "XXXXXXX", "XXXXXXX", ".XXXXX.", "..XXX..", "...X..."};
        for (int row = 0; row < heart.length; row++) {
            for (int col = 0; col < heart[row].length(); col++) {
                if (heart[row].charAt(col) == 'X') {
                    g.fill(x + col, y + row + 1, x + col + 1, y + row + 2, heartColor);
                }
            }
        }
        String likes = formatCount(post.likes() + (isLiked ? 1 : 0));
        drawSmall(g, likes, x + 10, y + 1, isLiked ? 0xFFE5484D : MachineScreenHelper.COLOR_TEXT_DIM);
        heartBoxes.add(new int[] {post.index(), x - 2, y - 1, 34, 10});

        int cx = x + 40;
        g.fill(cx, y + 1, cx + 7, y + 6, 0xFF6E7480);
        g.fill(cx + 1, y + 2, cx + 6, y + 5, 0xFF101318);
        g.fill(cx + 1, y + 6, cx + 3, y + 7, 0xFF6E7480);
        drawSmall(g, formatCount(post.replies()), cx + 10, y + 1, MachineScreenHelper.COLOR_TEXT_DIM);

        drawSmall(g, ">>", x + 78, y + 1, 0xFF6E7480);
    }

    private void drawSmall(GuiGraphics g, String text, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.7F, 0.7F, 1.0F);
        g.drawString(this.font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private static String formatCount(int n) {
        if (n < 1000) {
            return Integer.toString(n);
        }
        if (n < 1_000_000) {
            return String.format("%.1fk", n / 1000.0F);
        }
        return String.format("%.1fM", n / 1_000_000.0F);
    }

    /** Pastel sky, a sun and two hill layers, derived from the seed. */
    private void drawImage(GuiGraphics g, int x, int y, int w, int h, int seed) {
        Random r = new Random(seed);
        float hue = r.nextFloat();
        int skyTop = 0xFF000000 | Mth.hsvToRgb(hue, 0.45F, 0.95F);
        int skyBottom = 0xFF000000 | Mth.hsvToRgb((hue + 0.08F) % 1.0F, 0.25F, 1.0F);
        g.fillGradient(x, y, x + w, y + h, skyTop, skyBottom);

        int sunX = x + 8 + r.nextInt(Math.max(1, w - 24));
        int sunY = y + 4 + r.nextInt(8);
        g.fill(sunX, sunY, sunX + 8, sunY + 8, 0xFFFFF2B0);

        int far = 0xFF000000 | Mth.hsvToRgb((hue + 0.4F) % 1.0F, 0.4F, 0.6F);
        int near = 0xFF000000 | Mth.hsvToRgb((hue + 0.45F) % 1.0F, 0.5F, 0.4F);
        int farHeight = h / 3;
        int nearHeight = h / 5;
        for (int cx = 0; cx < w; cx += 4) {
            int fh = farHeight + r.nextInt(8) - 4;
            g.fill(x + cx, y + h - fh, Math.min(x + cx + 4, x + w), y + h, far);
        }
        for (int cx = 0; cx < w; cx += 4) {
            int nh = nearHeight + r.nextInt(6) - 3;
            g.fill(x + cx, y + h - nh, Math.min(x + cx + 4, x + w), y + h, near);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int[] box : heartBoxes) {
                if (mouseX >= box[1] && mouseX < box[1] + box[3] && mouseY >= box[2] && mouseY < box[2] + box[4]
                        && mouseY >= feedTop && mouseY < feedBottom) {
                    if (!liked.remove(box[0])) {
                        liked.add(box[0]);
                    }
                    return true;
                }
            }
        }
        velocity = 0.0F;
        return false;
    }

    @Override
    public boolean mouseDragged(double dragY) {
        scroll = Math.max(0.0F, scroll - (float) dragY);
        lastDragY = dragY;
        return true;
    }

    @Override
    public void mouseReleased() {
        velocity = (float) (-lastDragY * 1.4D);
        lastDragY = 0.0D;
    }

    @Override
    public boolean mouseScrolled(double delta) {
        velocity -= (float) delta * 7.0F;
        return true;
    }
}
