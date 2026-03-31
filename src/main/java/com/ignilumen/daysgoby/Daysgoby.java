package com.ignilumen.daysgoby;

import com.ignilumen.daysgoby.client.DaysgobyClientHooks;
import com.ignilumen.daysgoby.condition.ModConditions;
import com.ignilumen.daysgoby.config.DaysgobyConfig;
import com.ignilumen.daysgoby.config.WanderlustConfig;
import com.ignilumen.daysgoby.registry.ModAttachments;
import com.ignilumen.daysgoby.registry.ModComponents;
import com.ignilumen.daysgoby.registry.ModCreativeTabs;
import com.ignilumen.daysgoby.registry.ModItems;
import com.ignilumen.daysgoby.registry.ModMobEffects;
import com.ignilumen.daysgoby.registry.ModRecipeSerializers;
import com.ignilumen.daysgoby.specialweapon.SpecialWeaponCommands;
import com.ignilumen.daysgoby.specialweapon.SpecialWeaponEvents;
import com.ignilumen.daysgoby.util.ArmorLiningTooltipHandler;
import com.ignilumen.daysgoby.wanderlust.WanderlustEvents;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Daysgoby.MODID)
public final class Daysgoby {
    public static final String MODID = "daysgoby";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Daysgoby(IEventBus modEventBus, ModContainer modContainer) {
        DaysgobyConfig.register(modContainer, modEventBus);
        WanderlustConfig.register(modContainer);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            DaysgobyClientHooks.registerConfigScreen(modContainer);
            DaysgobyClientHooks.registerClientEventHandlers();
        }

        ModAttachments.register(modEventBus);
        ModComponents.register(modEventBus);
        ModConditions.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModMobEffects.register(modEventBus);
        ModRecipeSerializers.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(ArmorLiningTooltipHandler::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(SpecialWeaponCommands::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(SpecialWeaponEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(SpecialWeaponEvents::onLivingDamagePre);
        NeoForge.EVENT_BUS.addListener(SpecialWeaponEvents::onLivingDamagePost);
        NeoForge.EVENT_BUS.addListener(SpecialWeaponEvents::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(SpecialWeaponEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(WanderlustEvents::onPlayerTick);
    }
}
