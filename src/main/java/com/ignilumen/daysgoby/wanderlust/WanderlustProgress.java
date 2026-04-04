package com.ignilumen.daysgoby.wanderlust;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record WanderlustProgress(
        long lastResetDay,
        String lastChunkKey,
        int journeyScore,
        int chunkPointsAwarded,
        int journeyTickets,
        boolean dailyGoalCompleted,
        Set<String> todayVisitedChunks,
        Set<String> todayBiomes,
        Set<String> lifetimeBiomes,
        Set<String> todayStructures,
        Set<String> lifetimeStructures
) {
    private static final Codec<Set<String>> STRING_SET_CODEC = Codec.STRING.listOf()
            .xmap(values -> new LinkedHashSet<>(values), List::copyOf);

    public static final Codec<WanderlustProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("last_reset_day", -1L).forGetter(WanderlustProgress::lastResetDay),
            Codec.STRING.optionalFieldOf("last_chunk_key", "").forGetter(WanderlustProgress::lastChunkKey),
            Codec.INT.optionalFieldOf("journey_score", 0).forGetter(WanderlustProgress::journeyScore),
            Codec.INT.optionalFieldOf("chunk_points_awarded", 0).forGetter(WanderlustProgress::chunkPointsAwarded),
            Codec.INT.optionalFieldOf("journey_tickets", 0).forGetter(WanderlustProgress::journeyTickets),
            Codec.BOOL.optionalFieldOf("daily_goal_completed", false).forGetter(WanderlustProgress::dailyGoalCompleted),
            STRING_SET_CODEC.optionalFieldOf("today_visited_chunks", Set.of()).forGetter(WanderlustProgress::todayVisitedChunks),
            STRING_SET_CODEC.optionalFieldOf("today_biomes", Set.of()).forGetter(WanderlustProgress::todayBiomes),
            STRING_SET_CODEC.optionalFieldOf("lifetime_biomes", Set.of()).forGetter(WanderlustProgress::lifetimeBiomes),
            STRING_SET_CODEC.optionalFieldOf("today_structures", Set.of()).forGetter(WanderlustProgress::todayStructures),
            STRING_SET_CODEC.optionalFieldOf("lifetime_structures", Set.of()).forGetter(WanderlustProgress::lifetimeStructures)
    ).apply(instance, WanderlustProgress::new));

    public WanderlustProgress() {
        this(-1L, "", 0, 0, 0, false, Set.of(), Set.of(), Set.of(), Set.of(), Set.of());
    }

    public WanderlustProgress {
        lastChunkKey = lastChunkKey == null ? "" : lastChunkKey;
        journeyTickets = Math.max(0, journeyTickets);
        todayVisitedChunks = orderedCopy(todayVisitedChunks);
        todayBiomes = orderedCopy(todayBiomes);
        lifetimeBiomes = orderedCopy(lifetimeBiomes);
        todayStructures = orderedCopy(todayStructures);
        lifetimeStructures = orderedCopy(lifetimeStructures);
    }

    public WanderlustProgress resetForDay(long day) {
        return new WanderlustProgress(day, lastChunkKey, 0, 0, journeyTickets, false, Set.of(), Set.of(), lifetimeBiomes, Set.of(), lifetimeStructures);
    }

    public WanderlustProgress withLastChunkKey(String chunkKey) {
        return new WanderlustProgress(lastResetDay, chunkKey, journeyScore, chunkPointsAwarded, journeyTickets, dailyGoalCompleted,
                todayVisitedChunks, todayBiomes, lifetimeBiomes, todayStructures, lifetimeStructures);
    }

    public WanderlustProgress addVisitedChunk(String chunkKey) {
        Set<String> updated = new LinkedHashSet<>(todayVisitedChunks);
        updated.add(chunkKey);
        return new WanderlustProgress(lastResetDay, lastChunkKey, journeyScore, chunkPointsAwarded, journeyTickets, dailyGoalCompleted,
                updated, todayBiomes, lifetimeBiomes, todayStructures, lifetimeStructures);
    }

    public WanderlustProgress addTodayBiome(String biomeKey) {
        Set<String> updated = new LinkedHashSet<>(todayBiomes);
        updated.add(biomeKey);
        return new WanderlustProgress(lastResetDay, lastChunkKey, journeyScore, chunkPointsAwarded, journeyTickets, dailyGoalCompleted,
                todayVisitedChunks, updated, lifetimeBiomes, todayStructures, lifetimeStructures);
    }

    public WanderlustProgress addLifetimeBiome(String biomeKey) {
        Set<String> updated = new LinkedHashSet<>(lifetimeBiomes);
        updated.add(biomeKey);
        return new WanderlustProgress(lastResetDay, lastChunkKey, journeyScore, chunkPointsAwarded, journeyTickets, dailyGoalCompleted,
                todayVisitedChunks, todayBiomes, updated, todayStructures, lifetimeStructures);
    }

    public WanderlustProgress addTodayStructure(String structureKey) {
        Set<String> updated = new LinkedHashSet<>(todayStructures);
        updated.add(structureKey);
        return new WanderlustProgress(lastResetDay, lastChunkKey, journeyScore, chunkPointsAwarded, journeyTickets, dailyGoalCompleted,
                todayVisitedChunks, todayBiomes, lifetimeBiomes, updated, lifetimeStructures);
    }

    public WanderlustProgress addLifetimeStructure(String structureKey) {
        Set<String> updated = new LinkedHashSet<>(lifetimeStructures);
        updated.add(structureKey);
        return new WanderlustProgress(lastResetDay, lastChunkKey, journeyScore, chunkPointsAwarded, journeyTickets, dailyGoalCompleted,
                todayVisitedChunks, todayBiomes, lifetimeBiomes, todayStructures, updated);
    }

    public WanderlustProgress addJourneyScore(int amount) {
        return new WanderlustProgress(lastResetDay, lastChunkKey, journeyScore + amount, chunkPointsAwarded, journeyTickets, dailyGoalCompleted,
                todayVisitedChunks, todayBiomes, lifetimeBiomes, todayStructures, lifetimeStructures);
    }

    public WanderlustProgress withChunkPointsAwarded(int amount) {
        return new WanderlustProgress(lastResetDay, lastChunkKey, journeyScore, amount, journeyTickets, dailyGoalCompleted,
                todayVisitedChunks, todayBiomes, lifetimeBiomes, todayStructures, lifetimeStructures);
    }

    public WanderlustProgress addJourneyTickets(int amount) {
        return new WanderlustProgress(lastResetDay, lastChunkKey, journeyScore, chunkPointsAwarded, journeyTickets + Math.max(0, amount), dailyGoalCompleted,
                todayVisitedChunks, todayBiomes, lifetimeBiomes, todayStructures, lifetimeStructures);
    }

    public WanderlustProgress spendJourneyTickets(int amount) {
        return new WanderlustProgress(lastResetDay, lastChunkKey, journeyScore, chunkPointsAwarded, Math.max(0, journeyTickets - Math.max(0, amount)), dailyGoalCompleted,
                todayVisitedChunks, todayBiomes, lifetimeBiomes, todayStructures, lifetimeStructures);
    }

    public WanderlustProgress markDailyGoalCompleted() {
        return new WanderlustProgress(lastResetDay, lastChunkKey, journeyScore, chunkPointsAwarded, journeyTickets, true,
                todayVisitedChunks, todayBiomes, lifetimeBiomes, todayStructures, lifetimeStructures);
    }

    private static Set<String> orderedCopy(Set<String> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }
}
