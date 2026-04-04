package com.ignilumen.daysgoby.client;

import com.ignilumen.daysgoby.config.WanderlustConfig;
import com.ignilumen.daysgoby.registry.ModAttachments;
import com.ignilumen.daysgoby.wanderlust.JournalLotteryDrawPayload;
import com.ignilumen.daysgoby.wanderlust.WanderlustProgress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public final class WanderlustJournalScreen extends Screen {
    private static final int ENTRIES_PER_PAGE = 10;

    private final Player player;
    private ViewCategory selectedCategory = ViewCategory.TODAY_BIOMES;
    private int pageIndex;

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
        int panelLeft = this.width / 2 - 180;
        int panelTop = 30;
        int panelBottom = this.height - 36;

        addRenderableWidget(buildCategoryButton(panelLeft + 16, panelTop + 78, ViewCategory.TODAY_BIOMES));
        addRenderableWidget(buildCategoryButton(panelLeft + 126, panelTop + 78, ViewCategory.TODAY_STRUCTURES));
        addRenderableWidget(buildCategoryButton(panelLeft + 16, panelTop + 104, ViewCategory.LIFETIME_BIOMES));
        addRenderableWidget(buildCategoryButton(panelLeft + 126, panelTop + 104, ViewCategory.LIFETIME_STRUCTURES));

        previousPageButton = addRenderableWidget(Button.builder(Component.translatable("screen.daysgoby.travel_journal.previous"), button -> changePage(-1))
                .bounds(panelLeft + 20, panelBottom - 58, 60, 20)
                .build());
        nextPageButton = addRenderableWidget(Button.builder(Component.translatable("screen.daysgoby.travel_journal.next"), button -> changePage(1))
                .bounds(panelLeft + 170, panelBottom - 58, 60, 20)
                .build());

        if (WanderlustConfig.isJournalLotteryEnabled()) {
            int lotteryLeft = panelLeft + 252;
            drawOneButton = addRenderableWidget(Button.builder(Component.translatable("screen.daysgoby.travel_journal.draw_one"), button -> draw(JournalLotteryDrawPayload.DrawAmount.ONE))
                    .bounds(lotteryLeft, panelTop + 118, 92, 20)
                    .build());
            drawTenButton = addRenderableWidget(Button.builder(Component.translatable("screen.daysgoby.travel_journal.draw_ten"), button -> draw(JournalLotteryDrawPayload.DrawAmount.TEN))
                    .bounds(lotteryLeft, panelTop + 144, 92, 20)
                    .build());
            drawAllButton = addRenderableWidget(Button.builder(Component.translatable("screen.daysgoby.travel_journal.draw_all"), button -> draw(JournalLotteryDrawPayload.DrawAmount.ALL))
                    .bounds(lotteryLeft, panelTop + 170, 92, 20)
                    .build());
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(this.width / 2 - 50, this.height - 28, 100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        WanderlustProgress progress = currentProgress();
        updateButtonStates(progress);

        guiGraphics.fill(0, 0, this.width, this.height, 0xFF0B0D12);

        int panelLeft = this.width / 2 - 180;
        int panelTop = 30;
        int panelRight = this.width / 2 + 180;
        int panelBottom = this.height - 36;
        int leftColumn = panelLeft + 16;
        int rightColumn = panelLeft + 252;
        int white = 0xFFF3F3F3;

        guiGraphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xFF171C26);
        guiGraphics.fill(panelLeft - 2, panelTop - 2, panelRight + 2, panelTop, 0xFFE0B56A);
        guiGraphics.fill(panelLeft - 2, panelBottom, panelRight + 2, panelBottom + 2, 0xFFE0B56A);
        guiGraphics.fill(panelLeft - 2, panelTop, panelLeft, panelBottom, 0xFFE0B56A);
        guiGraphics.fill(panelRight, panelTop, panelRight + 2, panelBottom, 0xFFE0B56A);

        guiGraphics.fill(panelLeft + 12, panelTop + 126, panelLeft + 238, panelBottom - 66, 0xFF11161F);
        guiGraphics.fill(rightColumn - 8, panelTop + 92, panelRight - 16, panelTop + 206, 0xFF11161F);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFF7E8C6);

        int y = panelTop + 16;
        guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.player", this.player.getName()), leftColumn, y, white, true);
        y += 16;
        guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.score", progress.journeyScore()), leftColumn, y, white, true);
        y += 14;
        guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.goal", yesNo(progress.dailyGoalCompleted())), leftColumn, y, white, true);
        y += 14;
        guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.goal_remaining", Math.max(0, WanderlustConfig.SERVER.dailyGoalScore.getAsInt() - progress.journeyScore())), leftColumn, y, white, true);
        y += 14;
        guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.tickets", progress.journeyTickets()), leftColumn, y, 0xFFFFD387, true);

        guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.today_title"), rightColumn - 236, panelTop + 16, 0xFFFFD387, true);
        guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.today_chunks", progress.todayVisitedChunks().size()), rightColumn - 236, panelTop + 32, white, true);
        guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.today_biomes", progress.todayBiomes().size()), rightColumn - 236, panelTop + 46, white, true);
        guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.today_structures", progress.todayStructures().size()), rightColumn - 236, panelTop + 60, white, true);

        guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.lifetime_title"), rightColumn - 72, panelTop + 16, 0xFF9FD3FF, true);
        guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.lifetime_biomes", progress.lifetimeBiomes().size()), rightColumn - 72, panelTop + 32, white, true);
        guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.lifetime_structures", progress.lifetimeStructures().size()), rightColumn - 72, panelTop + 46, white, true);

        guiGraphics.drawString(this.font, Component.translatable(selectedCategory.titleKey), panelLeft + 20, panelTop + 132, 0xFFF7E8C6, true);
        renderEntries(guiGraphics, progress, panelLeft + 20, panelTop + 148, 204);

        if (WanderlustConfig.isJournalLotteryEnabled()) {
            guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.lottery_title"), rightColumn, panelTop + 100, 0xFFF7E8C6, true);
            guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.lottery_cost", WanderlustConfig.lotteryCostPerDraw()), rightColumn, panelTop + 116, white, true);
        } else {
            guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.lottery_disabled"), rightColumn - 4, panelTop + 116, 0xFFE49090, true);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Button buildCategoryButton(int x, int y, ViewCategory category) {
        return Button.builder(Component.translatable(category.buttonKey), button -> {
                    selectedCategory = category;
                    pageIndex = 0;
                })
                .bounds(x, y, 104, 20)
                .build();
    }

    private void changePage(int delta) {
        pageIndex += delta;
    }

    private void draw(JournalLotteryDrawPayload.DrawAmount drawAmount) {
        PacketDistributor.sendToServer(new JournalLotteryDrawPayload(drawAmount));
    }

    private void renderEntries(GuiGraphics guiGraphics, WanderlustProgress progress, int left, int top, int maxWidth) {
        List<String> entries = currentEntries(progress);
        int maxPage = Math.max(0, (entries.size() - 1) / ENTRIES_PER_PAGE);
        pageIndex = Mth.clamp(pageIndex, 0, maxPage);

        if (entries.isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.empty"), left, top, 0xFF97A6BE, false);
            return;
        }

        int start = pageIndex * ENTRIES_PER_PAGE;
        int end = Math.min(entries.size(), start + ENTRIES_PER_PAGE);
        int y = top;

        for (int index = start; index < end; index++) {
            String label = resolveEntryName(entries.get(index));
            String text = trimToWidth("- " + label, maxWidth);
            guiGraphics.drawString(this.font, text, left, y, 0xFFEAEAEA, false);
            y += 12;
        }

        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.daysgoby.travel_journal.page", pageIndex + 1, maxPage + 1),
                left + 76,
                this.height - 88,
                0xFF97A6BE,
                false
        );
    }

    private void updateButtonStates(WanderlustProgress progress) {
        List<String> entries = currentEntries(progress);
        int maxPage = Math.max(0, (entries.size() - 1) / ENTRIES_PER_PAGE);
        pageIndex = Mth.clamp(pageIndex, 0, maxPage);

        if (previousPageButton != null) {
            previousPageButton.active = pageIndex > 0;
        }
        if (nextPageButton != null) {
            nextPageButton.active = pageIndex < maxPage;
        }

        int tickets = progress.journeyTickets();
        int cost = WanderlustConfig.lotteryCostPerDraw();
        if (drawOneButton != null) {
            drawOneButton.active = tickets >= cost;
        }
        if (drawTenButton != null) {
            drawTenButton.active = tickets >= cost * 10;
        }
        if (drawAllButton != null) {
            drawAllButton.active = tickets >= cost;
        }
    }

    private WanderlustProgress currentProgress() {
        return this.player.getExistingData(ModAttachments.WANDERLUST_PROGRESS).orElse(new WanderlustProgress());
    }

    private List<String> currentEntries(WanderlustProgress progress) {
        return switch (selectedCategory) {
            case TODAY_BIOMES -> new ArrayList<>(progress.todayBiomes());
            case TODAY_STRUCTURES -> new ArrayList<>(progress.todayStructures());
            case LIFETIME_BIOMES -> new ArrayList<>(progress.lifetimeBiomes());
            case LIFETIME_STRUCTURES -> new ArrayList<>(progress.lifetimeStructures());
        };
    }

    private String resolveEntryName(String entryId) {
        ResourceLocation id = ResourceLocation.tryParse(entryId);
        if (id == null) {
            return entryId;
        }

        String translationKey = Util.makeDescriptionId(selectedCategory.isBiomeCategory ? "biome" : "structure", id);
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

    private String trimToWidth(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int availableWidth = Math.max(0, maxWidth - this.font.width(ellipsis));
        return this.font.plainSubstrByWidth(text, availableWidth) + ellipsis;
    }

    private Component yesNo(boolean value) {
        return Component.translatable(value ? "screen.daysgoby.travel_journal.yes" : "screen.daysgoby.travel_journal.no");
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
