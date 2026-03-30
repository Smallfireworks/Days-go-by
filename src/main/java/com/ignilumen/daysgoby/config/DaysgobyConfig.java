package com.ignilumen.daysgoby.config;

import com.ignilumen.daysgoby.module.ModModules;
import org.apache.commons.lang3.tuple.Pair;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class DaysgobyConfig {
    public static final String MODULES_FILE_NAME = "daysgoby-modules.toml";

    public static final Startup STARTUP;
    public static final ModConfigSpec STARTUP_SPEC;

    static {
        Pair<Startup, ModConfigSpec> startupSpec = new ModConfigSpec.Builder().configure(Startup::new);
        STARTUP = startupSpec.getLeft();
        STARTUP_SPEC = startupSpec.getRight();
    }

    private DaysgobyConfig() {}

    public static void register(ModContainer modContainer, IEventBus modEventBus) {
        modEventBus.addListener(DaysgobyConfig::onLoad);
        modEventBus.addListener(DaysgobyConfig::onReload);
        modContainer.registerConfig(ModConfig.Type.STARTUP, STARTUP_SPEC, MODULES_FILE_NAME);
    }

    private static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == STARTUP_SPEC) {
            ModModules.refreshFromConfig();
        }
    }

    private static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == STARTUP_SPEC) {
            ModModules.refreshFromConfig();
        }
    }

    public static final class Startup {
        public final ModConfigSpec.BooleanValue armorLining;
        public final ModConfigSpec.BooleanValue wanderlust;

        private Startup(ModConfigSpec.Builder builder) {
            builder.comment("Enable or disable gameplay modules. Changes require a game restart.")
                    .push("modules");

            armorLining = builder
                    .comment(
                            "Enable the armor lining module.",
                            "This module is automatically disabled when Tough As Nails is not installed."
                    )
                    .gameRestart()
                    .define("armorLining", true);

            wanderlust = builder
                    .comment(
                            "Enable the upcoming wanderlust module.",
                            "Reserved for the future exploration-focused module."
                    )
                    .gameRestart()
                    .define("wanderlust", true);

            builder.pop();
        }
    }
}
