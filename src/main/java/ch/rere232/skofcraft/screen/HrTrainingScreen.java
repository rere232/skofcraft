package ch.rere232.skofcraft.screen;

import ch.rere232.skofcraft.network.HrCompleteC2SPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * The training itself: slides on a timer, then a quiz, then a certificate. There is no close button and Escape does
 * nothing. A wrong answer doesn't fail you, it just locks the page for a while so you can reflect.
 */
@OnlyIn(Dist.CLIENT)
public class HrTrainingScreen extends Screen {
    private static final int SLIDES = 5;
    private static final int SLIDE_TICKS = 20 * 8;
    /** The correct answer to each question, counted from 0. */
    private static final int[] QUIZ_ANSWERS = {0, 2, 1};
    private static final int ANSWERS_PER_QUESTION = 3;
    private static final int REFLECTION_TICKS = 20 * 5;
    private static final int PAGES = SLIDES + QUIZ_ANSWERS.length + 1;

    private static final int PANEL_MAX_WIDTH = 340;
    private static final int PANEL_HEIGHT = 196;
    private static final int PADDING = 12;
    private static final int BUTTON_HEIGHT = 20;
    private static final int COLOR_PAPER = 0xFFF1EEE6;
    private static final int COLOR_HEADER = 0xFF2B4C7E;
    private static final int COLOR_TEXT = 0xFF2A2A2A;
    private static final int COLOR_MUTED = 0xFF70706A;
    private static final int COLOR_BAR = 0xFFD9D5C9;
    private static final int COLOR_WRONG = 0xFFB02A2A;

    private int page;
    private int waitTicks = SLIDE_TICKS;
    private boolean wrong;
    private final List<Button> buttons = new ArrayList<>();

    public HrTrainingScreen() {
        super(Component.translatable("hr.skofcraft.title"));
    }

    private int panelWidth() {
        return Math.min(PANEL_MAX_WIDTH, width - 20);
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return (height - PANEL_HEIGHT) / 2;
    }

    private boolean isQuiz() {
        return page >= SLIDES && page < SLIDES + QUIZ_ANSWERS.length;
    }

    private boolean isCertificate() {
        return page == PAGES - 1;
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        buttons.clear();
        int left = panelLeft() + PADDING;
        int inner = panelWidth() - PADDING * 2;
        int bottom = panelTop() + PANEL_HEIGHT - PADDING - BUTTON_HEIGHT;
        if (isQuiz()) {
            int question = page - SLIDES;
            for (int i = 0; i < ANSWERS_PER_QUESTION; i++) {
                int answer = i;
                addButton(Button.builder(Component.translatable("hr.skofcraft.quiz." + question + "." + i), b -> answer(answer))
                        .bounds(left, bottom - (ANSWERS_PER_QUESTION - 1 - i) * (BUTTON_HEIGHT + 2), inner, BUTTON_HEIGHT).build());
            }
        } else if (isCertificate()) {
            addButton(Button.builder(Component.translatable("hr.skofcraft.acknowledge"),
                    b -> SkofcraftNetwork.CHANNEL.sendToServer(new HrCompleteC2SPacket())).bounds(left, bottom, inner, BUTTON_HEIGHT).build());
        } else {
            addButton(Button.builder(Component.translatable("hr.skofcraft.next"), b -> nextPage())
                    .bounds(left + inner - 80, bottom, 80, BUTTON_HEIGHT).build());
        }
        updateButtons();
    }

    private void addButton(Button button) {
        buttons.add(addRenderableWidget(button));
    }

    private void updateButtons() {
        for (Button button : buttons) {
            button.active = waitTicks <= 0;
        }
    }

    private void nextPage() {
        page = Math.min(page + 1, PAGES - 1);
        waitTicks = page < SLIDES ? SLIDE_TICKS : 0;
        wrong = false;
        rebuild();
    }

    private void answer(int answer) {
        if (answer == QUIZ_ANSWERS[page - SLIDES]) {
            nextPage();
        } else {
            wrong = true;
            waitTicks = REFLECTION_TICKS;
            updateButtons();
        }
    }

    @Override
    public void tick() {
        if (waitTicks > 0) {
            waitTicks--;
            updateButtons();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xFF14161C);
        int left = panelLeft();
        int top = panelTop();
        int panel = panelWidth();
        int inner = panel - PADDING * 2;
        g.fill(left, top, left + panel, top + PANEL_HEIGHT, COLOR_PAPER);
        g.fill(left, top, left + panel, top + 22, COLOR_HEADER);
        g.drawString(font, title, left + PADDING, top + 7, 0xFFFFFFFF, false);
        Component progress = Component.translatable("hr.skofcraft.progress", page + 1, PAGES);
        g.drawString(font, progress, left + panel - PADDING - font.width(progress), top + 7, 0xFFD5DEEE, false);

        Component heading;
        Component body;
        if (isQuiz()) {
            heading = Component.translatable("hr.skofcraft.quiz.title");
            body = Component.translatable("hr.skofcraft.quiz." + (page - SLIDES) + ".question");
        } else if (isCertificate()) {
            heading = Component.translatable("hr.skofcraft.certificate.title");
            body = Component.translatable("hr.skofcraft.certificate.body", minecraft != null && minecraft.player != null ? minecraft.player.getName() : Component.empty());
        } else {
            heading = Component.translatable("hr.skofcraft.slide." + page + ".title");
            body = Component.translatable("hr.skofcraft.slide." + page + ".body");
        }
        g.drawString(font, heading.copy().withStyle(ChatFormatting.BOLD), left + PADDING, top + 30, COLOR_HEADER, false);
        g.drawWordWrap(font, body, left + PADDING, top + 44, inner, COLOR_TEXT);

        int bottom = top + PANEL_HEIGHT - PADDING;
        if (wrong) {
            g.drawWordWrap(font, Component.translatable("hr.skofcraft.quiz.wrong"), left + PADDING, top + 74, inner, COLOR_WRONG);
        }
        if (!isQuiz() && !isCertificate()) {
            int barWidth = inner - 90;
            float done = 1.0F - Mth.clamp((float) waitTicks / SLIDE_TICKS, 0.0F, 1.0F);
            g.fill(left + PADDING, bottom - 12, left + PADDING + barWidth, bottom - 8, COLOR_BAR);
            g.fill(left + PADDING, bottom - 12, left + PADDING + (int) (barWidth * done), bottom - 8, COLOR_HEADER);
            g.drawString(font, Component.translatable("hr.skofcraft.cannot_skip"), left + PADDING, bottom - 24, COLOR_MUTED, false);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
