package com.ignilumen.daysgoby.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

public final class DaysgobyClientHooks {
    private DaysgobyClientHooks() {}

    public static void registerConfigScreen(ModContainer modContainer) {
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (IConfigScreenFactory) (container, parent) -> new ConfigurationScreen(container, parent)
        );
    }

    public static void registerClientEventHandlers() {
        NeoForge.EVENT_BUS.addListener(SpecialWeaponClientRenderer::onRenderPlayerPost);
        NeoForge.EVENT_BUS.addListener(SpecialWeaponClientRenderer::onRenderLevelStage);
    }

    public static void openTravelJournal(Player player) {
        Minecraft.getInstance().setScreen(new WanderlustJournalScreen(Component.translatable("screen.daysgoby.travel_journal"), player));
    }
}
