package com.ignilumen.daysgoby.wanderlust;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ignilumen.daysgoby.Daysgoby;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;

public final class WanderlustLotteryManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE_PATH = FMLPaths.CONFIGDIR.get().resolve(Daysgoby.MODID).resolve("wanderlust-lottery.json");
    private static final ResourceLocation AIR_ID = ResourceLocation.withDefaultNamespace("air");
    private static final Object LOCK = new Object();

    private static LotteryDefinition cachedDefinition = LotteryDefinition.EMPTY;
    private static long cachedLastModified = Long.MIN_VALUE;

    private WanderlustLotteryManager() {}

    public static LotteryDefinition getDefinition() {
        synchronized (LOCK) {
            reloadIfNeeded();
            return cachedDefinition;
        }
    }

    private static void reloadIfNeeded() {
        try {
            ensureFileExists();
            long modified = Files.getLastModifiedTime(FILE_PATH).toMillis();
            if (modified == cachedLastModified) {
                return;
            }

            cachedDefinition = readDefinition();
            cachedLastModified = modified;
        } catch (IOException | RuntimeException exception) {
            Daysgoby.LOGGER.error("Failed to load wanderlust lottery config from {}", FILE_PATH, exception);
            cachedDefinition = LotteryDefinition.EMPTY;
            try {
                cachedLastModified = Files.exists(FILE_PATH) ? Files.getLastModifiedTime(FILE_PATH).toMillis() : Long.MIN_VALUE;
            } catch (IOException ignored) {
                cachedLastModified = Long.MIN_VALUE;
            }
        }
    }

    private static void ensureFileExists() throws IOException {
        if (Files.exists(FILE_PATH)) {
            return;
        }

        Files.createDirectories(FILE_PATH.getParent());
        Files.writeString(FILE_PATH, GSON.toJson(createDefaultJson()));
    }

    private static LotteryDefinition readDefinition() throws IOException {
        JsonObject root = GSON.fromJson(Files.readString(FILE_PATH), JsonObject.class);
        if (root == null || !root.has("entries") || !root.get("entries").isJsonArray()) {
            Daysgoby.LOGGER.warn("Wanderlust lottery config at {} is missing a valid entries array", FILE_PATH);
            return LotteryDefinition.EMPTY;
        }

        List<LotteryEntry> entries = new ArrayList<>();
        JsonArray rawEntries = root.getAsJsonArray("entries");
        for (JsonElement element : rawEntries) {
            if (!element.isJsonObject()) {
                continue;
            }

            LotteryEntry parsedEntry = parseEntry(element.getAsJsonObject());
            if (parsedEntry != null) {
                entries.add(parsedEntry);
            }
        }

        if (entries.isEmpty()) {
            Daysgoby.LOGGER.warn("Wanderlust lottery config at {} has no usable entries", FILE_PATH);
            return LotteryDefinition.EMPTY;
        }

        return new LotteryDefinition(List.copyOf(entries));
    }

    private static LotteryEntry parseEntry(JsonObject entry) {
        ResourceLocation itemId = ResourceLocation.tryParse(getString(entry, "item", ""));
        if (itemId == null) {
            return null;
        }

        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == BuiltInRegistries.ITEM.get(AIR_ID)) {
            Daysgoby.LOGGER.warn("Skipping unknown wanderlust lottery item {}", itemId);
            return null;
        }

        int weight = Math.max(1, getInt(entry, "weight", 1));
        int min = Math.max(1, getInt(entry, "min", 1));
        int max = Math.max(min, getInt(entry, "max", min));
        return new LotteryEntry(item, weight, min, max);
    }

    private static JsonObject createDefaultJson() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        JsonArray entries = new JsonArray();
        entries.add(createEntry("minecraft:bread", 30, 2, 5));
        entries.add(createEntry("minecraft:cooked_beef", 20, 1, 3));
        entries.add(createEntry("minecraft:torch", 20, 8, 24));
        entries.add(createEntry("minecraft:arrow", 16, 8, 24));
        entries.add(createEntry("minecraft:paper", 15, 4, 12));
        entries.add(createEntry("minecraft:string", 12, 2, 6));
        entries.add(createEntry("minecraft:emerald", 8, 1, 3));
        entries.add(createEntry("minecraft:golden_carrot", 5, 2, 4));
        entries.add(createEntry("minecraft:ender_pearl", 3, 1, 2));
        entries.add(createEntry("minecraft:name_tag", 2, 1, 1));
        entries.add(createEntry("minecraft:saddle", 1, 1, 1));
        entries.add(createEntry("minecraft:golden_apple", 1, 1, 1));
        root.add("entries", entries);
        return root;
    }

    private static JsonObject createEntry(String itemId, int weight, int min, int max) {
        JsonObject entry = new JsonObject();
        entry.addProperty("item", itemId);
        entry.addProperty("weight", weight);
        entry.addProperty("min", min);
        entry.addProperty("max", max);
        return entry;
    }

    private static String getString(JsonObject json, String key, String fallback) {
        JsonElement value = json.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : fallback;
    }

    private static int getInt(JsonObject json, String key, int fallback) {
        JsonElement value = json.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsInt() : fallback;
    }

    private record LotteryEntry(Item item, int weight, int minCount, int maxCount) {
        private ItemStack createReward(RandomSource random) {
            ItemStack stack = item.getDefaultInstance();
            int maxStackSize = Math.max(1, stack.getMaxStackSize());
            int count = minCount == maxCount ? minCount : minCount + random.nextInt(maxCount - minCount + 1);
            stack.setCount(Math.min(count, maxStackSize));
            return stack;
        }
    }

    public record LotteryDefinition(List<LotteryEntry> entries) {
        private static final LotteryDefinition EMPTY = new LotteryDefinition(List.of());

        public LotteryDefinition {
            entries = List.copyOf(entries);
        }

        public boolean isEmpty() {
            return entries.isEmpty();
        }

        public ItemStack draw(RandomSource random) {
            if (entries.isEmpty()) {
                return ItemStack.EMPTY;
            }

            int totalWeight = 0;
            for (LotteryEntry entry : entries) {
                totalWeight += entry.weight();
            }

            if (totalWeight <= 0) {
                return ItemStack.EMPTY;
            }

            int roll = random.nextInt(totalWeight);
            for (LotteryEntry entry : entries) {
                roll -= entry.weight();
                if (roll < 0) {
                    return entry.createReward(random);
                }
            }

            return entries.get(entries.size() - 1).createReward(random);
        }
    }
}
