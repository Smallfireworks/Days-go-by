package com.ignilumen.daysgoby.config;

import org.apache.commons.lang3.tuple.Pair;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class WanderlustConfig {
    public static final String FILE_NAME = "daysgoby-wanderlust-server.toml";

    public static final Server SERVER;
    public static final ModConfigSpec SERVER_SPEC;

    static {
        Pair<Server, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Server::new);
        SERVER = specPair.getLeft();
        SERVER_SPEC = specPair.getRight();
    }

    private WanderlustConfig() {}

    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC, FILE_NAME);
    }

    public static int tier1Score() {
        return SERVER.tier1Score.getAsInt();
    }

    public static int tier2Score() {
        return Math.max(tier1Score(), SERVER.tier2Score.getAsInt());
    }

    public static int tier3Score() {
        return Math.max(tier2Score(), SERVER.tier3Score.getAsInt());
    }

    public static final class Server {
        public final ModConfigSpec.IntValue chunksPerPoint;
        public final ModConfigSpec.IntValue dailyBiomeScore;
        public final ModConfigSpec.IntValue lifetimeBiomeBonus;
        public final ModConfigSpec.IntValue dailyStructureScore;
        public final ModConfigSpec.BooleanValue ignoreHiddenStructures;
        public final ModConfigSpec.IntValue dailyGoalScore;
        public final ModConfigSpec.IntValue tier1Score;
        public final ModConfigSpec.IntValue tier2Score;
        public final ModConfigSpec.IntValue tier3Score;
        public final ModConfigSpec.IntValue effectDurationSeconds;

        private Server(ModConfigSpec.Builder builder) {
            builder.comment("旅行模块的计分规则。")
                    .translation("daysgoby.configuration.scoring")
                    .push("scoring");

            chunksPerPoint = builder
                    .comment("每天跨越多少个唯一区块可以换算成 1 点旅程分。")
                    .translation("daysgoby.configuration.chunksPerPoint")
                    .defineInRange("chunksPerPoint", 4, 1, 64);

            dailyBiomeScore = builder
                    .comment("当天第一次发现某个群系时获得的旅程分。")
                    .translation("daysgoby.configuration.dailyBiomeScore")
                    .defineInRange("dailyBiomeScore", 1, 0, 32);

            lifetimeBiomeBonus = builder
                    .comment("生涯第一次发现某个群系时额外获得的旅程分。")
                    .translation("daysgoby.configuration.lifetimeBiomeBonus")
                    .defineInRange("lifetimeBiomeBonus", 1, 0, 32);

            dailyStructureScore = builder
                    .comment("当天第一次发现某个结构时获得的旅程分。")
                    .translation("daysgoby.configuration.dailyStructureScore")
                    .defineInRange("dailyStructureScore", 2, 0, 64);

            ignoreHiddenStructures = builder
                    .comment("忽略被标记为隐藏展示或隐藏定位的结构。")
                    .translation("daysgoby.configuration.ignoreHiddenStructures")
                    .define("ignoreHiddenStructures", true);

            builder.pop();

            builder.comment("旅行模块的分数阈值。")
                    .translation("daysgoby.configuration.goals")
                    .push("goals");

            dailyGoalScore = builder
                    .comment("完成每日远行所需的旅程分。")
                    .translation("daysgoby.configuration.dailyGoalScore")
                    .defineInRange("dailyGoalScore", 6, 1, 256);

            tier1Score = builder
                    .comment("第一层奖励阈值。")
                    .translation("daysgoby.configuration.tier1Score")
                    .defineInRange("tier1Score", 3, 1, 256);

            tier2Score = builder
                    .comment("第二层奖励阈值。")
                    .translation("daysgoby.configuration.tier2Score")
                    .defineInRange("tier2Score", 6, 1, 256);

            tier3Score = builder
                    .comment("第三层奖励阈值。")
                    .translation("daysgoby.configuration.tier3Score")
                    .defineInRange("tier3Score", 10, 1, 256);

            builder.pop();

            builder.comment("旅行模块的奖励效果设置。")
                    .translation("daysgoby.configuration.effects")
                    .push("effects");

            effectDurationSeconds = builder
                    .comment("每次获得旅程分后，旅行增益会被刷新到多少秒。")
                    .translation("daysgoby.configuration.effectDurationSeconds")
                    .defineInRange("effectDurationSeconds", 240, 10, 3600);

            builder.pop();
        }
    }
}
