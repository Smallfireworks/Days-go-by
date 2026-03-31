package com.ignilumen.daysgoby.client;

import com.ignilumen.daysgoby.wanderlust.WanderlustProgress;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class WanderlustJournalScreen extends Screen {
    private final Player player;
    private final WanderlustProgress progress;

    public WanderlustJournalScreen(Component title, Player player, WanderlustProgress progress) {
        super(title);
        this.player = player;
        this.progress = progress;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(this.width / 2 - 50, this.height - 28, 100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xFF0B0D12);

        int panelLeft = this.width / 2 - 144;
        int panelTop = 34;
        int panelRight = this.width / 2 + 144;
        int panelBottom = this.height - 40;
        guiGraphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xFF171C26);
        guiGraphics.fill(panelLeft - 2, panelTop - 2, panelRight + 2, panelTop, 0xFFE0B56A);
        guiGraphics.fill(panelLeft - 2, panelBottom, panelRight + 2, panelBottom + 2, 0xFFE0B56A);
        guiGraphics.fill(panelLeft - 2, panelTop, panelLeft, panelBottom, 0xFFE0B56A);
        guiGraphics.fill(panelRight, panelTop, panelRight + 2, panelBottom, 0xFFE0B56A);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 18, 0xFFF7E8C6);

        int left = this.width / 2 - 122;
        int y = 50;
        int color = 0xFFF3F3F3;

        guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.player", this.player.getName()), left, y, color, true);
        y += 18;
        guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.score", progress.journeyScore()), left, y, color, true);
        y += 14;
        guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.goal", yesNo(progress.dailyGoalCompleted())), left, y, color, true);
        y += 22;
        guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.today_title"), left, y, 0xFFFFD387, true);
        y += 16;
        guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.today_chunks", progress.todayVisitedChunks().size()), left, y, color, true);
        y += 14;
        guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.today_biomes", progress.todayBiomes().size()), left, y, color, true);
        y += 14;
        guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.today_structures", progress.todayStructures().size()), left, y, color, true);
        y += 22;
        guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.lifetime_title"), left, y, 0xFF9FD3FF, true);
        y += 16;
        guiGraphics.drawString(this.font, Component.translatable("screen.daysgoby.travel_journal.lifetime_biomes", progress.lifetimeBiomes().size()), left, y, color, true);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    private Component yesNo(boolean value) {
        return Component.translatable(value ? "screen.daysgoby.travel_journal.yes" : "screen.daysgoby.travel_journal.no");
    }
}
