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
        public final ModConfigSpec.BooleanValue enchantment;
        public final ModConfigSpec.BooleanValue specialWeapon;
        public final ModConfigSpec.BooleanValue wanderlust;

        private Startup(ModConfigSpec.Builder builder) {
            builder.comment("启用或禁用 Days go by 的功能模块。修改后需要重启游戏。")
                    .translation("daysgoby.configuration.modules")
                    .push("modules");

            armorLining = builder
                    .comment(
                            "启用盔甲内衬模块。",
                            "若未安装意志坚定，该模块会自动失效。"
                    )
                    .translation("daysgoby.configuration.armorLining")
                    .gameRestart()
                    .define("armorLining", true);

            enchantment = builder
                    .comment(
                            "启用附魔模块。",
                            "关闭后会禁用本模组附魔的效果和获取来源。"
                    )
                    .translation("daysgoby.configuration.enchantment")
                    .gameRestart()
                    .define("enchantment", true);

            specialWeapon = builder
                    .comment(
                            "启用特殊武器模块。",
                            "关闭后会禁用该模块的相关合成配方。"
                    )
                    .translation("daysgoby.configuration.specialWeapon")
                    .gameRestart()
                    .define("specialWeapon", true);

            wanderlust = builder
                    .comment(
                            "启用旅行模块。",
                            "该模块会鼓励玩家行动并探索世界。"
                    )
                    .translation("daysgoby.configuration.wanderlust")
                    .gameRestart()
                    .define("wanderlust", true);

            builder.pop();
        }
    }
}
