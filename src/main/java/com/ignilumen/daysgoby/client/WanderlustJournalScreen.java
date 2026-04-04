package com.ignilumen.daysgoby.client;

import com.ignilumen.daysgoby.config.WanderlustConfig;
import com.ignilumen.daysgoby.registry.ModAttachments;
import com.ignilumen.daysgoby.wanderlust.JournalLotteryDrawPayload;
import com.ignilumen.daysgoby.wanderlust.WanderlustProgress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public final class WanderlustJournalScreen extends Screen {
    private static final int MAX_PANEL_WIDTH = 430;
    private static final int PANEL_TOP = 14;
    private static final int PANEL_BOTTOM_MARGIN = 30;
    private static final int INNER_PADDING = 14;
    private static final int SECTION_GAP = 10;
    private static final int TAB_HEIGHT = 20;
    private static final int NAV_BUTTON_HEIGHT = 20;
    private static final int NAV_BUTTON_GAP = 4;
    private static final int LIST_ROW_HEIGHT = 14;
    private static final int MIN_LIST_ROWS = 4;
    private static final int MAX_LIST_ROWS = 9;
    private static final int LIST_NAV_WIDTH = 124;
    private static final int FOOTER_BUTTON_WIDTH = 60;

    private final Player player;
    private final Map<ViewCategory, Button> categoryButtons = new EnumMap<>(ViewCategory.class);

    private JournalPage selectedPage = JournalPage.RECORDS;
    private ViewCategory selectedCategory = ViewCategory.TODAY_BIOMES;
    private int pageIndex;

    private Button recordsPageButton;
    private Button lotteryPageButton;
    private Button previousPageButton;
    private Button nextPageButton;
    private Button drawOneButton;
    private Button drawTenButton;
    private Button drawAllButton;

    public WanderlustJournalScreen(Component title, Player player) {
        super(title);
        this.player = player;
    }

    @Override
    protected void init() {
        this.categoryButtons.clear();

        int tabWidth = 88;
        int tabGap = 8;
        int tabsStart = this.width / 2 - tabWidth - tabGap / 2;
        int tabsY = pageTabsTop();

        this.recordsPageButton = addRenderableWidget(Button.builder(Component.translatable("screen.daysgoby.travel_journal.records_page"),
                        button -> switchPage(JournalPage.RECORDS))
                .bounds(tabsStart, tabsY, tabWidth, TAB_HEIGHT)
                .build());
        this.lotteryPageButton = addRenderableWidget(Button.builder(Component.translatable("screen.daysgoby.travel_journal.lottery_page"),
                        button -> switchPage(JournalPage.LOTTERY))
                .bounds(tabsStart + tabWidth + tabGap, tabsY, tabWidth, TAB_HEIGHT)
                .build());

        int navigationLeft = navigationLeft();
        int navigationTop = recordsContentTop();
        int navigationWidth = navigationWidth();

        for (ViewCategory category : ViewCategory.values()) {
            int y = navigationTop + category.ordinal() * (NAV_BUTTON_HEIGHT + NAV_BUTTON_GAP);
            this.categoryButtons.put(category, addRenderableWidget(buildCategoryButton(navigationLeft, y, navigationWidth, category)));
        }

        int footerY = recordsFooterTop();
        int listLeft = listLeft();
        int listRight = listRight();

        this.previousPageButton = addRenderableWidget(Button.builder(Component.translatable("screen.daysgoby.travel_journal.previous"), button -> changePage(-1))
                .bounds(listLeft, footerY, FOOTER_BUTTON_WIDTH, TAB_HEIGHT)
                .build());
        this.nextPageButton = addRenderableWidget(Button.builder(Component.translatable("screen.daysgoby.travel_journal.next"), button -> changePage(1))
                .bounds(listRight - FOOTER_BUTTON_WIDTH, footerY, FOOTER_BUTTON_WIDTH, TAB_HEIGHT)
                .build());

        if (WanderlustConfig.isJournalLotteryEnabled()) {
            int lotteryButtonWidth = 112;
            int lotteryButtonLeft = this.width / 2 - lotteryButtonWidth / 2;
            int firstButtonY = lotteryPanelTop() + 48;

            this.drawOneButton = addRenderableWidget(Button.builder(Component.translatable("screen.daysgoby.travel_journal.draw_one"),
                            button -> draw(JournalLotteryDrawPayload.DrawAmount.ONE))
                    .bounds(lotteryButtonLeft, firstButtonY, lotteryButtonWidth, TAB_HEIGHT)
                    .build());
            this.drawTenButton = addRenderableWidget(Button.builder(Component.translatable("screen.daysgoby.travel_journal.draw_ten"),
                            button -> draw(JournalLotteryDrawPayload.DrawAmount.TEN))
                    .bounds(lotteryButtonLeft, firstButtonY + 24, lotteryButtonWidth, TAB_HEIGHT)
                    .build());
            this.drawAllButton = addRenderableWidget(Button.builder(Component.translatable("screen.daysgoby.travel_journal.draw_all"),
                            button -> draw(JournalLotteryDrawPayload.DrawAmount.ALL))
                    .bounds(lotteryButtonLeft, firstButtonY + 48, lotteryButtonWidth, TAB_HEIGHT)
                    .build());
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(this.width / 2 - 50, this.height - 24, 100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        WanderlustProgress progress = currentProgress();
        updateButtonStates(progress);

        int panelLeft = panelLeft();
        int panelTop = panelTop();
        int panelRight = panelRight();
        int panelBottom = panelBottom();
        int white = 0xFFF1F1F1;

        guiGraphics.fill(0, 0, this.width, this.height, 0xFF0B0D12);
        guiGraphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xFF171C26);
        guiGraphics.fill(panelLeft - 2, panelTop - 2, panelRight + 2, panelTop, 0xFFE0B56A);
        guiGraphics.fill(panelLeft - 2, panelBottom, panelRight + 2, panelBottom + 2, 0xFFE0B56A);
        guiGraphics.fill(panelLeft - 2, panelTop, panelLeft, panelBottom, 0xFFE0B56A);
        guiGraphics.fill(panelRight, panelTop, panelRight + 2, panelBottom, 0xFFE0B56A);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 6, 0xFFF7E8C6);
        renderSummary(guiGraphics, progress);

        if (this.selectedPage == JournalPage.RECORDS) {
            renderRecordsPage(guiGraphics, progress, white);
        } else {
            renderLotteryPage(guiGraphics, progress, white);
        }

        for (Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderSummary(GuiGraphics guiGraphics, WanderlustProgress progress) {
        int left = innerLeft();
        int right = innerRight();
        int middle = left + (right - left) / 2;
        int top = summaryTop();
        int bottom = summaryBottom();

        drawInsetBox(guiGraphics, left, top, middle - SECTION_GAP / 2, bottom);
        drawInsetBox(guiGraphics, middle + SECTION_GAP / 2, top, right, bottom);

        int leftWidth = middle - SECTION_GAP / 2 - left - 12;
        int rightWidth = right - (middle + SECTION_GAP / 2) - 12;
        int lineY = top + 7;
        int lineStep = 10;

        drawTrimmed(guiGraphics, Component.translatable("screen.daysgoby.travel_journal.player", this.player.getName()).getString(),
                left + 6, lineY, leftWidth, 0xFFF5E7C7);
        drawTrimmed(guiGraphics, Component.translatable("screen.daysgoby.travel_journal.score", progress.journeyScore()).getString(),
                middle + SECTION_GAP / 2 + 6, lineY, rightWidth, 0xFFF5E7C7);

        lineY += lineStep;
        drawTrimmed(guiGraphics, Component.translatable("screen.daysgoby.travel_journal.goal", yesNo(progress.dailyGoalCompleted())).getString(),
                left + 6, lineY, leftWidth, 0xFFEAEAEA);

        if (this.selectedPage == JournalPage.RECORDS) {
            drawTrimmed(guiGraphics, Component.translatable("screen.daysgoby.travel_journal.today_chunks", progress.todayVisitedChunks().size()).getString(),
                    middle + SECTION_GAP / 2 + 6, lineY, rightWidth, 0xFFEAEAEA);
        } else {
            drawTrimmed(guiGraphics, Component.translatable("screen.daysgoby.travel_journal.tickets", progress.journeyTickets()).getString(),
                    middle + SECTION_GAP / 2 + 6, lineY, rightWidth, 0xFFFFD387);
        }

        lineY += lineStep;
        drawTrimmed(guiGraphics, Component.translatable("screen.daysgoby.travel_journal.today_discoveries",
                        progress.todayBiomes().size(), progress.todayStructures().size()).getString(),
                left + 6, lineY, leftWidth, 0xFFE0C078);

        if (this.selectedPage == JournalPage.RECORDS) {
            drawTrimmed(guiGraphics, Component.translatable("screen.daysgoby.travel_journal.lifetime_discoveries",
                            progress.lifetimeBiomes().size(), progress.lifetimeStructures().size()).getString(),
                    middle + SECTION_GAP / 2 + 6, lineY, rightWidth, 0xFF9FD3FF);
        } else {
            drawTrimmed(guiGraphics, Component.translatable("screen.daysgoby.travel_journal.lottery_cost", WanderlustConfig.lotteryCostPerDraw()).getString(),
                    middle + SECTION_GAP / 2 + 6, lineY, rightWidth, 0xFF9FD3FF);
        }
    }

    private void renderRecordsPage(GuiGraphics guiGraphics, WanderlustProgress progress, int white) {
        int listLeft = listLeft();
        int listRight = listRight();
        int listTop = listTop();
        int listBottom = listBottom();

        drawInsetBox(guiGraphics, navigationLeft(), recordsContentTop(), navigationLeft() + navigationWidth(), listBottom);
        drawInsetBox(guiGraphics, listLeft, listTop, listRight, listBottom);

        guiGraphics.drawString(this.font, Component.translatable(selectedCategory.titleKey), listLeft, listTop - 12, 0xFFF7E8C6, false);
        renderEntries(guiGraphics, progress, listLeft + 8, listTop + 8, listRight - listLeft - 16, listBottom - listTop - 16);

        int centerX = listLeft + (listRight - listLeft) / 2;
        guiGraphics.drawCenteredString(this.font, pageLabel(progress), centerX, recordsFooterTop() + 6, 0xFF97A6BE);
        guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.category_title"), navigationLeft() + 8, recordsContentTop() - 12, white, false);
    }

    private void renderLotteryPage(GuiGraphics guiGraphics, WanderlustProgress progress, int white) {
        int lotteryLeft = lotteryPanelLeft();
        int lotteryRight = lotteryPanelRight();
        int lotteryTop = lotteryPanelTop();
        int lotteryBottom = lotteryPanelBottom();
        int centerX = this.width / 2;
        int availableDraws = progress.journeyTickets() / WanderlustConfig.lotteryCostPerDraw();

        drawInsetBox(guiGraphics, lotteryLeft, lotteryTop, lotteryRight, lotteryBottom);
        guiGraphics.drawCenteredString(this.font, Component.translatable("screen.daysgoby.travel_journal.lottery_title"), centerX, lotteryTop + 10, 0xFFF7E8C6);
        guiGraphics.drawCenteredString(this.font, Component.translatable("screen.daysgoby.travel_journal.tickets", progress.journeyTickets()), centerX, lotteryTop + 24, 0xFFFFD387);
        guiGraphics.drawCenteredString(this.font, Component.translatable("screen.daysgoby.travel_journal.max_draws", availableDraws), centerX, lotteryTop + 38, 0xFFEAEAEA);

        if (!WanderlustConfig.isJournalLotteryEnabled()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("screen.daysgoby.travel_journal.lottery_disabled"), centerX, lotteryTop + 76, 0xFFE49090);
            return;
        }
    }

    private void renderEntries(GuiGraphics guiGraphics, WanderlustProgress progress, int left, int top, int maxWidth, int maxHeight) {
        List<String> entries = currentEntries(progress);
        int entriesPerPage = entriesPerPage(maxHeight);
        int maxPage = Math.max(0, (entries.size() - 1) / entriesPerPage);
        this.pageIndex = Mth.clamp(this.pageIndex, 0, maxPage);

        if (entries.isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.empty"), left, top, 0xFF97A6BE, false);
            return;
        }

        int start = this.pageIndex * entriesPerPage;
        int end = Math.min(entries.size(), start + entriesPerPage);
        int y = top;

        for (int index = start; index < end; index++) {
            String label = resolveEntryName(entries.get(index));
            guiGraphics.drawString(this.font, trimToWidth("- " + label, maxWidth), left, y, 0xFFEAEAEA, false);
            y += LIST_ROW_HEIGHT;
        }
    }

    private void updateButtonStates(WanderlustProgress progress) {
        List<String> entries = currentEntries(progress);
        int maxPage = Math.max(0, (entries.size() - 1) / entriesPerPage(listBottom() - listTop() - 16));
        this.pageIndex = Mth.clamp(this.pageIndex, 0, maxPage);

        boolean recordsPage = this.selectedPage == JournalPage.RECORDS;
        boolean lotteryEnabled = WanderlustConfig.isJournalLotteryEnabled();

        this.recordsPageButton.active = this.selectedPage != JournalPage.RECORDS;
        this.lotteryPageButton.active = this.selectedPage != JournalPage.LOTTERY;

        for (Map.Entry<ViewCategory, Button> entry : this.categoryButtons.entrySet()) {
            entry.getValue().visible = recordsPage;
            entry.getValue().active = recordsPage && entry.getKey() != this.selectedCategory;
        }

        this.previousPageButton.visible = recordsPage;
        this.previousPageButton.active = recordsPage && this.pageIndex > 0;
        this.nextPageButton.visible = recordsPage;
        this.nextPageButton.active = recordsPage && this.pageIndex < maxPage;

        int tickets = progress.journeyTickets();
        int cost = WanderlustConfig.lotteryCostPerDraw();
        boolean showLotteryButtons = this.selectedPage == JournalPage.LOTTERY && lotteryEnabled;

        if (this.drawOneButton != null) {
            this.drawOneButton.visible = showLotteryButtons;
            this.drawOneButton.active = showLotteryButtons && tickets >= cost;
        }
        if (this.drawTenButton != null) {
            this.drawTenButton.visible = showLotteryButtons;
            this.drawTenButton.active = showLotteryButtons && tickets >= cost * 10;
        }
        if (this.drawAllButton != null) {
            this.drawAllButton.visible = showLotteryButtons;
            this.drawAllButton.active = showLotteryButtons && tickets >= cost;
        }
    }

    private Button buildCategoryButton(int x, int y, int width, ViewCategory category) {
        return Button.builder(Component.translatable(category.buttonKey), button -> {
                    this.selectedCategory = category;
                    this.pageIndex = 0;
                })
                .bounds(x, y, width, NAV_BUTTON_HEIGHT)
                .build();
    }

    private void switchPage(JournalPage page) {
        this.selectedPage = page;
    }

    private void changePage(int delta) {
        this.pageIndex += delta;
    }

    private void draw(JournalLotteryDrawPayload.DrawAmount drawAmount) {
        PacketDistributor.sendToServer(new JournalLotteryDrawPayload(drawAmount));
    }

    private WanderlustProgress currentProgress() {
        return this.player.getExistingData(ModAttachments.WANDERLUST_PROGRESS).orElse(new WanderlustProgress());
    }

    private List<String> currentEntries(WanderlustProgress progress) {
        return switch (this.selectedCategory) {
            case TODAY_BIOMES -> new ArrayList<>(progress.todayBiomes());
            case TODAY_STRUCTURES -> new ArrayList<>(progress.todayStructures());
            case LIFETIME_BIOMES -> new ArrayList<>(progress.lifetimeBiomes());
            case LIFETIME_STRUCTURES -> new ArrayList<>(progress.lifetimeStructures());
        };
    }

    private Component pageLabel(WanderlustProgress progress) {
        List<String> entries = currentEntries(progress);
        int maxPage = Math.max(0, (entries.size() - 1) / entriesPerPage(listBottom() - listTop() - 16));
        return Component.translatable("screen.daysgoby.travel_journal.page", this.pageIndex + 1, maxPage + 1);
    }

    private String resolveEntryName(String entryId) {
        ResourceLocation id = ResourceLocation.tryParse(entryId);
        if (id == null) {
            return entryId;
        }

        String translationKey = Util.makeDescriptionId(this.selectedCategory.isBiomeCategory ? "biome" : "structure", id);
        if (I18n.exists(translationKey)) {
            return I18n.get(translationKey);
        }

        String prettyPath = Arrays.stream(id.getPath().split("_"))
                .filter(part -> !part.isBlank())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .reduce((left, right) -> left + " " + right)
                .orElse(entryId);
        return "minecraft".equals(id.getNamespace()) ? prettyPath : prettyPath + " [" + id.getNamespace() + "]";
    }

    private void drawInsetBox(GuiGraphics guiGraphics, int left, int top, int right, int bottom) {
        guiGraphics.fill(left, top, right, bottom, 0xFF11161F);
        guiGraphics.fill(left, top, right, top + 1, 0xFF2A3442);
        guiGraphics.fill(left, bottom - 1, right, bottom, 0xFF2A3442);
        guiGraphics.fill(left, top, left + 1, bottom, 0xFF2A3442);
        guiGraphics.fill(right - 1, top, right, bottom, 0xFF2A3442);
    }

    private void drawTrimmed(GuiGraphics guiGraphics, String text, int x, int y, int maxWidth, int color) {
        guiGraphics.drawString(this.font, trimToWidth(text, maxWidth), x, y, color, false);
    }

    private String trimToWidth(String text, int maxWidth) {
        if (maxWidth <= 0 || this.font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int availableWidth = Math.max(0, maxWidth - this.font.width(ellipsis));
        return this.font.plainSubstrByWidth(text, availableWidth) + ellipsis;
    }

    private Component yesNo(boolean value) {
        return Component.translatable(value ? "screen.daysgoby.travel_journal.yes" : "screen.daysgoby.travel_journal.no");
    }

    private int panelWidth() {
        return Math.min(MAX_PANEL_WIDTH, this.width - 24);
    }

    private int panelLeft() {
        return (this.width - panelWidth()) / 2;
    }

    private int panelTop() {
        return PANEL_TOP;
    }

    private int panelRight() {
        return panelLeft() + panelWidth();
    }

    private int panelBottom() {
        return this.height - PANEL_BOTTOM_MARGIN;
    }

    private int innerLeft() {
        return panelLeft() + INNER_PADDING;
    }

    private int innerRight() {
        return panelRight() - INNER_PADDING;
    }

    private int pageTabsTop() {
        return panelTop() + 10;
    }

    private int summaryTop() {
        return pageTabsTop() + TAB_HEIGHT + 8;
    }

    private int summaryBottom() {
        return summaryTop() + 38;
    }

    private int recordsContentTop() {
        return summaryBottom() + 12;
    }

    private int recordsFooterTop() {
        return panelBottom() - 24;
    }

    private int navigationLeft() {
        return innerLeft();
    }

    private int navigationWidth() {
        return Math.min(LIST_NAV_WIDTH, innerRight() - innerLeft() - 120);
    }

    private int listLeft() {
        return navigationLeft() + navigationWidth() + SECTION_GAP;
    }

    private int listRight() {
        return innerRight();
    }

    private int listTop() {
        return recordsContentTop();
    }

    private int listBottom() {
        return recordsFooterTop() - 6;
    }

    private int lotteryPanelLeft() {
        return innerLeft();
    }

    private int lotteryPanelRight() {
        return innerRight();
    }

    private int lotteryPanelTop() {
        return summaryBottom() + 12;
    }

    private int lotteryPanelBottom() {
        return panelBottom() - 8;
    }

    private int entriesPerPage(int maxHeight) {
        return Mth.clamp(maxHeight / LIST_ROW_HEIGHT, MIN_LIST_ROWS, MAX_LIST_ROWS);
    }

    private enum JournalPage {
        RECORDS,
        LOTTERY
    }

    private enum ViewCategory {
        TODAY_BIOMES("screen.daysgoby.travel_journal.category.today_biomes", "screen.daysgoby.travel_journal.tab.today_biomes", true),
        TODAY_STRUCTURES("screen.daysgoby.travel_journal.category.today_structures", "screen.daysgoby.travel_journal.tab.today_structures", false),
        LIFETIME_BIOMES("screen.daysgoby.travel_journal.category.lifetime_biomes", "screen.daysgoby.travel_journal.tab.lifetime_biomes", true),
        LIFETIME_STRUCTURES("screen.daysgoby.travel_journal.category.lifetime_structures", "screen.daysgoby.travel_journal.tab.lifetime_structures", false);

        private final String titleKey;
        private final String buttonKey;
        private final boolean isBiomeCategory;

        ViewCategory(String titleKey, String buttonKey, boolean isBiomeCategory) {
            this.titleKey = titleKey;
            this.buttonKey = buttonKey;
            this.isBiomeCategory = isBiomeCategory;
        }
    }
}
