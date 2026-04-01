package com.ignilumen.daysgoby.enchantment;

import com.ignilumen.daysgoby.Daysgoby;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.AddPackFindersEvent;

public final class ConfiguredEnchantmentPack {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PACK_ID = Daysgoby.MODID + ":configured_enchantments";

    private ConfiguredEnchantmentPack() {}

    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }

        Path packRoot = FMLPaths.CONFIGDIR.get().resolve(Daysgoby.MODID).resolve("generated").resolve("configured_enchantments");

        try {
            writePack(packRoot);
        } catch (IOException exception) {
            Daysgoby.LOGGER.error("Failed to write configured enchantment pack at {}", packRoot, exception);
            return;
        }

        String version = ModList.get().getModContainerById(Daysgoby.MODID)
                .orElseThrow()
                .getModInfo()
                .getVersion()
                .toString();

        Pack pack = Pack.readMetaAndCreate(
                new PackLocationInfo(
                        PACK_ID,
                        Component.literal("Daysgoby Configured Enchantments"),
                        PackSource.BUILT_IN,
                        Optional.of(new net.minecraft.server.packs.repository.KnownPack(Daysgoby.MODID, "configured_enchantments", version))
                ),
                new PathPackResources.PathResourcesSupplier(packRoot),
                PackType.SERVER_DATA,
                new PackSelectionConfig(true, Pack.Position.TOP, false)
        );

        if (pack == null) {
            Daysgoby.LOGGER.warn("Failed to create configured enchantment pack from {}", packRoot);
            return;
        }

        event.addRepositorySource(consumer -> consumer.accept(pack));
    }

    private static void writePack(Path packRoot) throws IOException {
        writeJson(packRoot.resolve("pack.mcmeta"), createPackMetadata());
        writeJson(packRoot.resolve(Path.of("data", Daysgoby.MODID, "enchantment", "shit_rain.json")), createShitRainEnchantment());
        writeJson(packRoot.resolve(tagPath("in_enchanting_table")), createTagFile(EnchantmentAvailability.canAppearInEnchantingTable(ModEnchantments.SHIT_RAIN), ModEnchantments.SHIT_RAIN.location().toString()));
        writeJson(packRoot.resolve(tagPath("on_random_loot")), createTagFile(EnchantmentAvailability.canGenerateOnRandomLoot(ModEnchantments.SHIT_RAIN), ModEnchantments.SHIT_RAIN.location().toString()));
        writeJson(packRoot.resolve(tagPath("tradeable")), createTagFile(EnchantmentAvailability.canBeTradeable(ModEnchantments.SHIT_RAIN), ModEnchantments.SHIT_RAIN.location().toString()));
        writeJson(packRoot.resolve(tagPath("on_traded_equipment")), createTagFile(EnchantmentAvailability.canGenerateOnTradedEquipment(ModEnchantments.SHIT_RAIN), ModEnchantments.SHIT_RAIN.location().toString()));
        writeJson(packRoot.resolve(tagPath("on_mob_spawn_equipment")), createTagFile(EnchantmentAvailability.canGenerateOnMobSpawnEquipment(ModEnchantments.SHIT_RAIN), ModEnchantments.SHIT_RAIN.location().toString()));
    }

    private static JsonObject createPackMetadata() {
        JsonObject root = new JsonObject();
        JsonObject pack = new JsonObject();
        pack.addProperty("description", "Daysgoby configured enchantment tags");
        pack.addProperty("pack_format", SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA));
        root.add("pack", pack);
        return root;
    }

    private static JsonObject createTagFile(boolean includeEntry, String entry) {
        JsonObject root = new JsonObject();
        JsonArray values = new JsonArray();
        root.addProperty("replace", false);
        if (includeEntry) {
            values.add(entry);
        }
        root.add("values", values);
        return root;
    }

    private static JsonObject createShitRainEnchantment() {
        JsonObject root = new JsonObject();
        JsonObject description = new JsonObject();
        JsonObject minCost = new JsonObject();
        JsonObject maxCost = new JsonObject();
        JsonArray slots = new JsonArray();

        description.addProperty("translate", "enchantment.daysgoby.shit_rain");
        minCost.addProperty("base", 8);
        minCost.addProperty("per_level_above_first", 12);
        maxCost.addProperty("base", 24);
        maxCost.addProperty("per_level_above_first", 12);
        slots.add("legs");

        root.addProperty("anvil_cost", 2);
        root.add("description", description);
        root.add("min_cost", minCost);
        root.add("max_cost", maxCost);
        root.addProperty("max_level", 1);
        root.add("slots", slots);
        root.addProperty("supported_items", "#minecraft:enchantable/leg_armor");
        root.addProperty("weight", 2);
        return root;
    }

    private static Path tagPath(String tagName) {
        return Path.of("data", "minecraft", "tags", "enchantment", tagName + ".json");
    }

    private static void writeJson(Path path, JsonObject json) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(json));
    }
}
