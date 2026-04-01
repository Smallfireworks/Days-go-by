package com.ignilumen.daysgoby.config;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class EnchantmentConfig {
    public static final String FILE_NAME = "daysgoby-enchantments.toml";

    public static final Startup STARTUP;
    public static final ModConfigSpec STARTUP_SPEC;

    static {
        Pair<Startup, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Startup::new);
        STARTUP = specPair.getLeft();
        STARTUP_SPEC = specPair.getRight();
    }

    private EnchantmentConfig() {}

    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.STARTUP, STARTUP_SPEC, FILE_NAME);
    }

    public static final class Startup {
        public final EnchantmentOptions shitRain;

        private Startup(ModConfigSpec.Builder builder) {
            builder.comment("控制各个附魔的启用状态和获取来源。修改后需要重启游戏。")
                    .translation("daysgoby.configuration.enchantments")
                    .push("enchantments");

            shitRain = new EnchantmentOptions(builder, "shitRain", "daysgoby.configuration.shitRain");

            builder.pop();
        }
    }

    public static final class EnchantmentOptions {
        public final ModConfigSpec.BooleanValue enabled;
        public final ModConfigSpec.BooleanValue inEnchantingTable;
        public final ModConfigSpec.BooleanValue onRandomLoot;
        public final ModConfigSpec.BooleanValue tradeable;
        public final ModConfigSpec.BooleanValue onTradedEquipment;
        public final ModConfigSpec.BooleanValue onMobSpawnEquipment;
        public final ModConfigSpec.ConfigValue<List<? extends String>> affectedMobWhitelist;

        private EnchantmentOptions(ModConfigSpec.Builder builder, String key, String translationKey) {
            builder.comment("该附魔的细化配置。")
                    .translation(translationKey)
                    .push(key);

            enabled = builder
                    .comment("启用该附魔。关闭后其效果会失效，且不会再出现在任何获取来源中。")
                    .translation(translationKey + ".enabled")
                    .gameRestart()
                    .define("enabled", true);

            inEnchantingTable = builder
                    .comment("允许该附魔出现在附魔台。")
                    .translation(translationKey + ".inEnchantingTable")
                    .gameRestart()
                    .define("inEnchantingTable", true);

            onRandomLoot = builder
                    .comment("允许该附魔通过随机战利品附魔书等途径生成。")
                    .translation(translationKey + ".onRandomLoot")
                    .gameRestart()
                    .define("onRandomLoot", true);

            tradeable = builder
                    .comment("允许该附魔出现在村民附魔书交易中。")
                    .translation(translationKey + ".tradeable")
                    .gameRestart()
                    .define("tradeable", true);

            onTradedEquipment = builder
                    .comment("允许该附魔出现在带附魔的交易装备上。")
                    .translation(translationKey + ".onTradedEquipment")
                    .gameRestart()
                    .define("onTradedEquipment", false);

            onMobSpawnEquipment = builder
                    .comment("允许该附魔出现在怪物自然生成的装备上。")
                    .translation(translationKey + ".onMobSpawnEquipment")
                    .gameRestart()
                    .define("onMobSpawnEquipment", false);

            affectedMobWhitelist = builder
                    .comment(
                            "受该附魔恶心/躲避效果影响的生物白名单。",
                            "留空时表示所有非 boss 生物都会生效。",
                            "填写实体 ID，例如 [\"minecraft:zombie\", \"minecraft:skeleton\"]。"
                    )
                    .translation(translationKey + ".affectedMobWhitelist")
                    .gameRestart()
                    .defineListAllowEmpty("affectedMobWhitelist", List.of(), () -> "minecraft:zombie", EnchantmentOptions::isValidEntityId);

            builder.pop();
        }

        private static boolean isValidEntityId(Object value) {
            return value instanceof String string && ResourceLocation.tryParse(string) != null;
        }
    }
}
