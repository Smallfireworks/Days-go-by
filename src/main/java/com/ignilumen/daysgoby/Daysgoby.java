package com.ignilumen.daysgoby;

import com.ignilumen.daysgoby.compat.ToughAsNailsCompat;
import com.ignilumen.daysgoby.registry.ModComponents;
import com.ignilumen.daysgoby.registry.ModCreativeTabs;
import com.ignilumen.daysgoby.registry.ModItems;
import com.ignilumen.daysgoby.registry.ModRecipeSerializers;
import com.ignilumen.daysgoby.util.ArmorLiningTooltipHandler;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Daysgoby.MODID)
public final class Daysgoby {
    public static final String MODID = "daysgoby";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Daysgoby(IEventBus modEventBus, ModContainer ignoredModContainer) {
        ModComponents.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModRecipeSerializers.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(ArmorLiningTooltipHandler::onItemTooltip);

        if (ModList.get().isLoaded(ToughAsNailsCompat.MOD_ID)) {
            ToughAsNailsCompat.init();
        }
    }
}