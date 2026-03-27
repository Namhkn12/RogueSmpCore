package com.roguesmp.dungeon.repository.impl;

import com.google.gson.*;
import com.roguesmp.dungeon.constant.DataConfig;
import com.roguesmp.dungeon.constant.LootEntryType;
import com.roguesmp.dungeon.data.LootEntry;
import com.roguesmp.dungeon.data.LootPool;
import com.roguesmp.dungeon.data.LootTable;
import com.roguesmp.dungeon.dto.DataResult;
import com.roguesmp.dungeon.exception.impl.data.DataLoadException;
import com.roguesmp.dungeon.repository.ILootTableRepository;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;

public class LootTableRepository implements ILootTableRepository {

    private final File lootFolder;
    private final String namespace;
    private final Gson gson;
    private final Logger logger;

    public LootTableRepository(@NotNull Plugin plugin, @NotNull Gson gson) {
        this.lootFolder = new File(plugin.getDataFolder(), DataConfig.getLootTableFolder());
        this.namespace = "rogue";
        this.gson = gson;
        this.logger = plugin.getLogger();

        if (!lootFolder.exists()) {
            lootFolder.mkdirs();
        }
    }

    @Override
    public @NotNull DataResult<Map<String, LootTable>> loadAll() {
        Map<String, LootTable> result = new HashMap<>();
        List<String> errors = new ArrayList<>();

        if (!lootFolder.exists() || !lootFolder.isDirectory()) {
            throw new DataLoadException(lootFolder.getName(), null);
        }

        collectJsonFiles(lootFolder, result, errors);

        logger.info("[LootTable] Loaded " + result.size() + " loot tables."
                + (errors.isEmpty() ? "" : " (" + errors.size() + " errors)"));

        return new DataResult<>(result, errors);
    }

    @Override
    public @Nullable LootTable loadById(@NotNull String id) {
        String withoutNamespace = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        File file = new File(lootFolder, withoutNamespace + DataConfig.JSON_TYPE);

        if (!file.exists()) {
            logger.warning("[LootTable] File not found for id '" + id + "': " + file.getPath());
            return null;
        }

        List<String> errors = new ArrayList<>();
        LootTable table = parseFile(file, errors);

        errors.forEach(e -> logger.warning("[LootTable] " + e));
        return table;
    }

    /**
     * Recursively walks {@code folder}, collecting all {@code .json} files.
     * Subdirectories are reflected in the derived loot table ID path.
     */
    private void collectJsonFiles(
            @NotNull File folder,
            @NotNull Map<String, LootTable> result,
            @NotNull List<String> errors
    ) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                collectJsonFiles(file, result, errors);
            } else if (file.getName().endsWith(DataConfig.JSON_TYPE)) {
                LootTable table = parseFile(file, errors);
                if (table != null) {
                    result.put(table.getId(), table);
                }
            }
        }
    }

    private @Nullable LootTable parseFile(@NotNull File file, @NotNull List<String> errors) {
        try (Reader reader = Files.newBufferedReader(file.toPath())) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            if (json == null) {
                errors.add(file.getName() + " - Empty or invalid JSON");
                return null;
            }
            return parseTable(json, file, errors);
        } catch (JsonParseException | IOException e) {
            errors.add(file.getName() + " - " + e.getMessage());
            return null;
        }
    }

    private @Nullable LootTable parseTable(
            @NotNull JsonObject json,
            @NotNull File file,
            @NotNull List<String> errors
    ) {
        String id = json.has("id") && json.get("id").isJsonPrimitive()
                ? json.get("id").getAsString()
                : deriveIdFromFile(file);

        if (!json.has("pools") || !json.get("pools").isJsonArray()) {
            errors.add(file.getName() + " - Missing 'pools' array");
            return null;
        }

        List<LootPool> pools = new ArrayList<>();
        for (JsonElement poolElement : json.get("pools").getAsJsonArray()) {
            if (!poolElement.isJsonObject()) {
                errors.add(file.getName() + " - Pool entry is not an object");
                continue;
            }
            LootPool pool = parsePool(poolElement.getAsJsonObject(), file, errors);
            if (pool != null) pools.add(pool);
        }

        if (pools.isEmpty()) {
            errors.add(file.getName() + " - No valid pools found");
            return null;
        }

        return new LootTable(id, pools);
    }

    private @Nullable LootPool parsePool(
            @NotNull JsonObject json,
            @NotNull File file,
            @NotNull List<String> errors
    ) {
        int rolls = getInt(json, "rolls", 1);
        double bonusRolls = getDouble(json, "bonus_rolls", 0.0);

        if (!json.has("entries") || !json.get("entries").isJsonArray()) {
            errors.add(file.getName() + " - Pool missing 'entries' array");
            return null;
        }

        List<LootEntry> entries = new ArrayList<>();
        for (JsonElement entryElement : json.get("entries").getAsJsonArray()) {
            if (!entryElement.isJsonObject()) continue;
            LootEntry entry = parseEntry(entryElement.getAsJsonObject(), file, errors);
            if (entry != null) entries.add(entry);
        }

        if (entries.isEmpty()) {
            errors.add(file.getName() + " - Pool has no valid entries");
            return null;
        }

        return new LootPool(rolls, bonusRolls, entries);
    }

    private @Nullable LootEntry parseEntry(
            @NotNull JsonObject json,
            @NotNull File file,
            @NotNull List<String> errors
    ) {
        if (!json.has("type")) {
            errors.add(file.getName() + " - Entry missing 'type'");
            return null;
        }

        String typeStr = json.get("type").getAsString().toUpperCase();
        LootEntryType type;
        try {
            type = LootEntryType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            errors.add(file.getName() + " - Unknown entry type '" + typeStr + "'");
            return null;
        }

        int weight = getInt(json, "weight", 1);

        return switch (type) {
            case ITEM -> {
                if (!json.has("item_id")) {
                    errors.add(file.getName() + " - ITEM entry missing 'item_id'");
                    yield null;
                }
                yield LootEntry.builder(LootEntryType.ITEM, weight)
                        .itemId(json.get("item_id").getAsString())
                        .amount(getInt(json, "min_amount", 1), getInt(json, "max_amount", 1))
                        .build();
            }
            case LOOT_TABLE -> {
                if (!json.has("name")) {
                    errors.add(file.getName() + " - LOOT_TABLE entry missing 'name'");
                    yield null;
                }
                yield LootEntry.builder(LootEntryType.LOOT_TABLE, weight)
                        .nestedTableId(json.get("name").getAsString())
                        .build();
            }
            case EMPTY -> LootEntry.builder(LootEntryType.EMPTY, weight).build();
        };
    }


    /**
     * Derives loot table ID from the file path relative to the loot_tables root.
     * e.g. {@code .../loot_tables/dungeons/dungeon_a.json} → {@code "rogue:dungeons/dungeon_a"}
     */
    private String deriveIdFromFile(@NotNull File file) {
        String relative = lootFolder.toURI().relativize(file.toURI()).getPath();
        String withoutExt = relative.replaceAll("\\.json$", "");
        return namespace + ":" + withoutExt;
    }

    private int getInt(@NotNull JsonObject json, @NotNull String key, int defaultVal) {
        if (!json.has(key)) return defaultVal;
        JsonElement el = json.get(key);
        return el.isJsonPrimitive() ? el.getAsInt() : defaultVal;
    }

    private double getDouble(@NotNull JsonObject json, @NotNull String key, double defaultVal) {
        if (!json.has(key)) return defaultVal;
        JsonElement el = json.get(key);
        return el.isJsonPrimitive() ? el.getAsDouble() : defaultVal;
    }
}