package com.ignilumen.daysgoby.client;

import com.ignilumen.daysgoby.wanderlust.WanderlustProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class DaysgobyClientHooks {
    private DaysgobyClientHooks() {}

    public static void registerConfigScreen(ModContainer modContainer) {
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (IConfigScreenFactory) (container, parent) -> new ConfigurationScreen(container, parent)
        );
    }

    public static void openTravelJournal(Player player, WanderlustProgress progress) {
        Minecraft.getInstance().setScreen(new WanderlustJournalScreen(Component.translatable("screen.daysgoby.travel_journal"), player, progress));
    }
}
