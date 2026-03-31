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
            builder.comment("Scoring rules for the wanderlust exploration module.")
                    .push("scoring");

            chunksPerPoint = builder
                    .comment("How many unique daily chunks across all dimensions are needed for one journey point.")
                    .defineInRange("chunksPerPoint", 4, 1, 64);

            dailyBiomeScore = builder
                    .comment("Journey points awarded when a biome is discovered for the first time today.")
                    .defineInRange("dailyBiomeScore", 1, 0, 32);

            lifetimeBiomeBonus = builder
                    .comment("Additional journey points awarded when a biome is discovered for the first time ever by that player.")
                    .defineInRange("lifetimeBiomeBonus", 1, 0, 32);

            dailyStructureScore = builder
                    .comment("Journey points awarded when a structure is discovered for the first time today.")
                    .defineInRange("dailyStructureScore", 2, 0, 64);

            ignoreHiddenStructures = builder
                    .comment("Ignore structures tagged as hidden from displayers or locator selection.")
                    .define("ignoreHiddenStructures", true);

            builder.pop();

            builder.comment("Journey score thresholds.")
                    .push("goals");

            dailyGoalScore = builder
                    .comment("Journey score required to complete the daily wanderlust goal.")
                    .defineInRange("dailyGoalScore", 6, 1, 256);

            tier1Score = builder
                    .comment("Journey score threshold for the first reward tier.")
                    .defineInRange("tier1Score", 3, 1, 256);

            tier2Score = builder
                    .comment("Journey score threshold for the second reward tier.")
                    .defineInRange("tier2Score", 6, 1, 256);

            tier3Score = builder
                    .comment("Journey score threshold for the third reward tier.")
                    .defineInRange("tier3Score", 10, 1, 256);

            builder.pop();

            builder.comment("Reward effect settings.")
                    .push("effects");

            effectDurationSeconds = builder
                    .comment("How long the wanderlust reward effects are refreshed for whenever score increases.")
                    .defineInRange("effectDurationSeconds", 240, 10, 3600);

            builder.pop();
        }
    }
}
