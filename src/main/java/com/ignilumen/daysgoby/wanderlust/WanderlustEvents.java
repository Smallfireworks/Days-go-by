package com.ignilumen.daysgoby.wanderlust;

import com.ignilumen.daysgoby.config.WanderlustConfig;
import com.ignilumen.daysgoby.module.ModModules;
import com.ignilumen.daysgoby.registry.ModAttachments;

import java.util.Objects;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class WanderlustEvents {
    private static final TagKey<Structure> HIDDEN_FROM_DISPLAYERS = TagKey.create(
            Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath("c", "hidden_from_displayers")
    );
    private static final TagKey<Structure> HIDDEN_FROM_LOCATOR_SELECTION = TagKey.create(
            Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath("c", "hidden_from_locator_selection")
    );

    private WanderlustEvents() {}

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!ModModules.isWanderlustEnabled()) {
            return;
        }

        Player player = event.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ServerLevel level = serverPlayer.serverLevel();
        WanderlustProgress original = serverPlayer.getData(ModAttachments.WANDERLUST_PROGRESS);
        WanderlustProgress progress = resetIfNeeded(serverPlayer, original);

        ChunkPos chunkPos = serverPlayer.chunkPosition();
        String chunkKey = toChunkKey(level, chunkPos);
        if (Objects.equals(chunkKey, progress.lastChunkKey())) {
            if (!progress.equals(original)) {
                serverPlayer.setData(ModAttachments.WANDERLUST_PROGRESS, progress);
            }
            return;
        }

        progress = progress.withLastChunkKey(chunkKey);
        int scoreBefore = progress.journeyScore();
        int tierBefore = getTier(scoreBefore);
        JourneyFeedback feedback = new JourneyFeedback();

        if (!progress.todayVisitedChunks().contains(chunkKey)) {
            progress = progress.addVisitedChunk(chunkKey);

            int chunkPoints = progress.todayVisitedChunks().size() / WanderlustConfig.SERVER.chunksPerPoint.getAsInt();
            int gainedChunkPoints = chunkPoints - progress.chunkPointsAwarded();
            if (gainedChunkPoints > 0) {
                progress = progress.withChunkPointsAwarded(chunkPoints).addJourneyScore(gainedChunkPoints);
                feedback.chunkPointGained = true;
            }
        }

        Optional<ResourceKey<Biome>> biomeKey = level.getBiome(serverPlayer.blockPosition()).unwrapKey();
        if (biomeKey.isPresent()) {
            String biomeId = biomeKey.get().location().toString();
            if (!progress.todayBiomes().contains(biomeId)) {
                progress = progress.addTodayBiome(biomeId).addJourneyScore(WanderlustConfig.SERVER.dailyBiomeScore.getAsInt());
                feedback.progressPriority = Math.max(feedback.progressPriority, 2);
                feedback.progressMessage = Component.translatable("actionbar.daysgoby.wanderlust.new_biome_today");
            }
            if (!progress.lifetimeBiomes().contains(biomeId)) {
                progress = progress.addLifetimeBiome(biomeId).addJourneyScore(WanderlustConfig.SERVER.lifetimeBiomeBonus.getAsInt());
                feedback.progressPriority = Math.max(feedback.progressPriority, 4);
                feedback.progressMessage = Component.translatable("actionbar.daysgoby.wanderlust.new_biome_lifetime");
            }
        }

        StructureStart structureStart = level.structureManager().getStructureWithPieceAt(serverPlayer.blockPosition(), WanderlustEvents::shouldCountStructure);
        if (structureStart.isValid()) {
            Registry<Structure> structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
            ResourceLocation structureId = structureRegistry.getKey(structureStart.getStructure());
            if (structureId != null) {
                String structureKey = structureId.toString();
                if (!progress.todayStructures().contains(structureKey)) {
                    progress = progress.addTodayStructure(structureKey)
                            .addJourneyScore(WanderlustConfig.SERVER.dailyStructureScore.getAsInt());
                    if (feedback.progressPriority < 3) {
                        feedback.progressPriority = 3;
                        feedback.progressMessage = Component.translatable("actionbar.daysgoby.wanderlust.new_structure");
                    }
                }
                if (!progress.lifetimeStructures().contains(structureKey)) {
                    progress = progress.addLifetimeStructure(structureKey);
                }
            }
        }

        boolean dailyGoalJustCompleted = !progress.dailyGoalCompleted()
                && progress.journeyScore() >= WanderlustConfig.SERVER.dailyGoalScore.getAsInt();
        if (dailyGoalJustCompleted) {
            progress = progress.markDailyGoalCompleted();
            int grantedTickets = WanderlustConfig.ticketPerDailyGoal();
            if (grantedTickets > 0) {
                progress = progress.addJourneyTickets(grantedTickets);
            }
        }

        if (!progress.equals(original)) {
            serverPlayer.setData(ModAttachments.WANDERLUST_PROGRESS, progress);
        }

        int scoreAfter = progress.journeyScore();
        if (scoreAfter > scoreBefore) {
            applyTierEffects(serverPlayer, getTier(scoreAfter));
        }

        if (feedback.progressMessage != null) {
            serverPlayer.displayClientMessage(feedback.progressMessage, true);
        } else if (scoreAfter > scoreBefore && feedback.chunkPointGained) {
            serverPlayer.displayClientMessage(Component.translatable("actionbar.daysgoby.wanderlust.new_land"), true);
        }

        int tierAfter = getTier(scoreAfter);
        if (dailyGoalJustCompleted) {
            serverPlayer.displayClientMessage(Component.translatable("message.daysgoby.wanderlust.daily_goal_complete"), false);
            int grantedTickets = WanderlustConfig.ticketPerDailyGoal();
            if (grantedTickets > 0) {
                serverPlayer.displayClientMessage(Component.translatable("message.daysgoby.wanderlust.tickets_gained", grantedTickets), false);
            }
        } else if (tierAfter > tierBefore) {
            serverPlayer.displayClientMessage(Component.translatable("message.daysgoby.wanderlust.tier_" + tierAfter), false);
        }
    }

    private static final class JourneyFeedback {
        private boolean chunkPointGained;
        private int progressPriority;
        private Component progressMessage;
    }

    private static WanderlustProgress resetIfNeeded(ServerPlayer player, WanderlustProgress progress) {
        ServerLevel overworld = player.serverLevel().getServer().getLevel(Level.OVERWORLD);
        long currentDay = (overworld != null ? overworld.getDayTime() : player.level().getDayTime()) / 24000L;
        return progress.lastResetDay() == currentDay ? progress : progress.resetForDay(currentDay);
    }

    private static String toChunkKey(ServerLevel level, ChunkPos chunkPos) {
        return level.dimension().location() + "|" + chunkPos.x + "|" + chunkPos.z;
    }

    private static boolean shouldCountStructure(Holder<Structure> holder) {
        if (!WanderlustConfig.SERVER.ignoreHiddenStructures.getAsBoolean()) {
            return true;
        }
        return !holder.is(HIDDEN_FROM_DISPLAYERS) && !holder.is(HIDDEN_FROM_LOCATOR_SELECTION);
    }

    private static int getTier(int score) {
        if (score >= WanderlustConfig.tier3Score()) {
            return 3;
        }
        if (score >= WanderlustConfig.tier2Score()) {
            return 2;
        }
        if (score >= WanderlustConfig.tier1Score()) {
            return 1;
        }
        return 0;
    }

    private static void applyTierEffects(ServerPlayer player, int tier) {
        if (tier <= 0) {
            return;
        }

        int duration = WanderlustConfig.SERVER.effectDurationSeconds.getAsInt() * 20;
        player.addEffect(createEffect(MobEffects.MOVEMENT_SPEED, duration, tier >= 3 ? 1 : 0));

        if (tier >= 2) {
            player.addEffect(createEffect(MobEffects.JUMP, duration, 0));
        }

        if (tier >= 3) {
            player.addEffect(createEffect(MobEffects.LUCK, duration, 0));
        }
    }

    private static MobEffectInstance createEffect(Holder<MobEffect> effect, int duration, int amplifier) {
        return new MobEffectInstance(effect, duration, amplifier, false, true, true);
    }
}
