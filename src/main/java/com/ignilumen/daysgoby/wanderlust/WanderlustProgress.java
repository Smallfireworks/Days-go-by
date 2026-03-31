package com.ignilumen.daysgoby.wanderlust;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record WanderlustProgress(
        long lastResetDay,
        String lastChunkKey,
        int journeyScore,
        int chunkPointsAwarded,
        boolean dailyGoalCompleted,
        Set<String> todayVisitedChunks,
        Set<String> todayBiomes,
        Set<String> lifetimeBiomes,
        Set<String> todayStructures
) {
    private static final Codec<Set<String>> STRING_SET_CODEC = Codec.STRING.listOf()
            .xmap(values -> new LinkedHashSet<>(values), List::copyOf);

    public static final Codec<WanderlustProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("last_reset_day", -1L).forGetter(WanderlustProgress::lastResetDay),
            Codec.STRING.optionalFieldOf("last_chunk_key", "").forGetter(WanderlustProgress::lastChunkKey),
            Codec.INT.optionalFieldOf("journey_score", 0).forGetter(WanderlustProgress::journeyScore),
            Codec.INT.optionalFieldOf("chunk_points_awarded", 0).forGetter(WanderlustProgress::chunkPointsAwarded),
            Codec.BOOL.optionalFieldOf("daily_goal_completed", false).forGetter(WanderlustProgress::dailyGoalCompleted),
            STRING_SET_CODEC.optionalFieldOf("today_visited_chunks", Set.of()).forGetter(WanderlustProgress::todayVisitedChunks),
            STRING_SET_CODEC.optionalFieldOf("today_biomes", Set.of()).forGetter(WanderlustProgress::todayBiomes),
            STRING_SET_CODEC.optionalFieldOf("lifetime_biomes", Set.of()).forGetter(WanderlustProgress::lifetimeBiomes),
            STRING_SET_CODEC.optionalFieldOf("today_structures", Set.of()).forGetter(WanderlustProgress::todayStructures)
    ).apply(instance, WanderlustProgress::new));

    public WanderlustProgress() {
        this(-1L, "", 0, 0, false, Set.of(), Set.of(), Set.of(), Set.of());
    }

    public WanderlustProgress {
        lastChunkKey = lastChunkKey == null ? "" : lastChunkKey;
        todayVisitedChunks = Set.copyOf(todayVisitedChunks);
        todayBiomes = Set.copyOf(todayBiomes);
        lifetimeBiomes = Set.copyOf(lifetimeBiomes);
        todayStructures = Set.copyOf(todayStructures);
    }

    public WanderlustProgress resetForDay(long day) {
        return new WanderlustProgress(day, lastChunkKey, 0, 0, false, Set.of(), Set.of(), lifetimeBiomes, Set.of());
    }

    public WanderlustProgress withLastChunkKey(String chunkKey) {
        return new WanderlustProgress(lastResetDay, chunkKey, journeyScore, chunkPointsAwarded, dailyGoalCompleted,
                todayVisitedChunks, todayBiomes, lifetimeBiomes, todayStructures);
    }

    public WanderlustProgress addVisitedChunk(String chunkKey) {
        Set<String> updated = new LinkedHashSet<>(todayVisitedChunks);
        updated.add(chunkKey);
        return new WanderlustProgress(lastResetDay, lastChunkKey, journeyScore, chunkPointsAwarded, dailyGoalCompleted,
                updated, todayBiomes, lifetimeBiomes, todayStructures);
    }

    public WanderlustProgress addTodayBiome(String biomeKey) {
        Set<String> updated = new LinkedHashSet<>(todayBiomes);
        updated.add(biomeKey);
        return new WanderlustProgress(lastResetDay, lastChunkKey, journeyScore, chunkPointsAwarded, dailyGoalCompleted,
                todayVisitedChunks, updated, lifetimeBiomes, todayStructures);
    }

    public WanderlustProgress addLifetimeBiome(String biomeKey) {
        Set<String> updated = new LinkedHashSet<>(lifetimeBiomes);
        updated.add(biomeKey);
        return new WanderlustProgress(lastResetDay, lastChunkKey, journeyScore, chunkPointsAwarded, dailyGoalCompleted,
                todayVisitedChunks, todayBiomes, updated, todayStructures);
    }

    public WanderlustProgress addTodayStructure(String structureKey) {
        Set<String> updated = new LinkedHashSet<>(todayStructures);
        updated.add(structureKey);
        return new WanderlustProgress(lastResetDay, lastChunkKey, journeyScore, chunkPointsAwarded, dailyGoalCompleted,
                todayVisitedChunks, todayBiomes, lifetimeBiomes, updated);
    }

    public WanderlustProgress addJourneyScore(int amount) {
        return new WanderlustProgress(lastResetDay, lastChunkKey, journeyScore + amount, chunkPointsAwarded, dailyGoalCompleted,
                todayVisitedChunks, todayBiomes, lifetimeBiomes, todayStructures);
    }

    public WanderlustProgress withChunkPointsAwarded(int amount) {
        return new WanderlustProgress(lastResetDay, lastChunkKey, journeyScore, amount, dailyGoalCompleted,
                todayVisitedChunks, todayBiomes, lifetimeBiomes, todayStructures);
    }

    public WanderlustProgress markDailyGoalCompleted() {
        return new WanderlustProgress(lastResetDay, lastChunkKey, journeyScore, chunkPointsAwarded, true,
                todayVisitedChunks, todayBiomes, lifetimeBiomes, todayStructures);
    }
}
