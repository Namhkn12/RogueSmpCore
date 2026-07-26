package com.roguesmp.registry;

import com.google.gson.JsonElement;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.JsonOps;
import com.roguesmp.tag.SmpTag;
import com.roguesmp.utils.Utils;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class Registry<T> {

    private static final Codec<List<String>> TAG_CODEC = Codec.listOf(Codec.STRING);

    private final Map<String, T> entries = new HashMap<>();
    private final Map<String, T> unmodifiableEntries = Collections.unmodifiableMap(entries);
    private final Map<String, SmpTag<T>> tags = new HashMap<>();
    private final Map<String, SmpTag<T>> unmodifiableTags = Collections.unmodifiableMap(tags);
    private final String locationKey; // Subfolder name inside data folder (e.g., "items", "recipes")
    private final Codec<T> codec;

    private static final List<Registry<?>> DATA_REGISTRIES = new ArrayList<>();

    // Constructor for Data-Driven Registries
    public Registry(String locationKey, Codec<T> codec) {
        this.locationKey = locationKey;
        this.codec = codec;
        DATA_REGISTRIES.add(this);
    }

    /**
     * Constructor for registries whose entries are populated in code (not loaded from a bulk *.json
     * folder), but which should still auto-discover tag files from {@code <locationKey>/tags/*.json}
     * (e.g. wrapping an enum like {@code Enchants} so it can be tagged).
     */
    public Registry(String locationKey) {
        this.locationKey = locationKey;
        this.codec = null;
        DATA_REGISTRIES.add(this);
    }

    // Constructor for In-Memory Only Registries
    public Registry() {
        this.locationKey = null;
        this.codec = null;
    }

    /**
     * Loads all .json files from the subfolder matching this registry's locationKey.
     */
    public @Blocking void loadFrom(RogueSmpCore plugin) {
        if (locationKey == null || codec == null) return;

        File folder = new File(plugin.getDataFolder(), locationKey);
        if (!folder.exists()) {
            folder.mkdirs();
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (files == null) return;

        int count = 0;
        for (File file : files) {
            // Strip ".json" extension to use as registry ID (e.g., "fire_sword.json" -> "fire_sword")
            String id = file.getName().substring(0, file.getName().length() - 5);

            try (FileReader reader = new FileReader(file)) {
                JsonElement json = Utils.GSON.fromJson(reader, JsonElement.class);
                if (json == null) continue;

                DataResult<T> result = codec.decode(json, JsonOps.INSTANCE);
                if (result.isSuccess()) {
                    register(id, result.result());
                    count++;
                } else {
                    RogueSmpCore.LOGGER.error("Failed to decode [{}] in '{}': {}", id, locationKey, result.error());
                }
            } catch (Exception e) {
                RogueSmpCore.LOGGER.error("Error reading file '{}' in '{}': {}", file.getName(), locationKey, e.getMessage());
            }
        }

        RogueSmpCore.LOGGER.info("Loaded {} entries into registry '{}'", count, locationKey);
    }

    /**
     * Loads ALL data-driven registries from plugin data folder
     */
    public static void loadAll(RogueSmpCore plugin) {
        for (Registry<?> registry : DATA_REGISTRIES) {
            registry.loadFrom(plugin);
        }
    }

    /**
     * Loads every {@code *.json} file under {@code <dataFolder>/<locationKey>/tags/} as an
     * {@link SmpTag} of this registry's type, auto-registered by filename and resolved against
     * this registry's own {@link #get(String)}. Drop a new file in that folder and it's
     * automatically picked up next load — no manual registration needed.
     * <p>
     * Must run after this registry's own entries are loaded/registered, since tag resolution
     * looks entries up by id via {@link #get(String)}.
     */
    public @Blocking void loadTagsFrom(RogueSmpCore plugin) {
        if (locationKey == null) return;

        File folder = new File(plugin.getDataFolder(), locationKey + "/tags");
        if (!folder.exists()) {
            folder.mkdirs();
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (files == null) return;

        tags.clear();
        for (File file : files) {
            String id = file.getName().substring(0, file.getName().length() - 5);

            try (FileReader reader = new FileReader(file)) {
                JsonElement json = Utils.GSON.fromJson(reader, JsonElement.class);
                if (json == null) continue;

                DataResult<List<String>> result = TAG_CODEC.decode(json, JsonOps.INSTANCE);
                if (result.isSuccess()) {
                    tags.put(id.toLowerCase(), new SmpTag<>(id, result.result(), this::get));
                } else {
                    RogueSmpCore.LOGGER.error("Failed to decode tag [{}] in '{}/tags': {}", id, locationKey, result.error());
                }
            } catch (Exception e) {
                RogueSmpCore.LOGGER.error("Error reading tag file '{}' in '{}/tags': {}", file.getName(), locationKey, e.getMessage());
            }
        }

        // Resolve every tag's elements (and any nested #tag references, scoped to this registry's own tags)
        for (SmpTag<T> tag : tags.values()) {
            tag.resolve(tags::get, new HashSet<>());
        }

        RogueSmpCore.LOGGER.info("Loaded {} tags into registry '{}'", tags.size(), locationKey);
    }

    /**
     * Loads tags for ALL data-driven registries. Must run after {@link #loadAll(RogueSmpCore)}.
     */
    public static void loadAllTags(RogueSmpCore plugin) {
        for (Registry<?> registry : DATA_REGISTRIES) {
            registry.loadTagsFrom(plugin);
        }
    }

    /**
     * Saves a specific entry to disk as a .json file.
     *
     * @param plugin The plugin instance (used to locate the data folder)
     * @param id     The registry key/filename (without .json extension)
     * @return true if the save succeeded, false otherwise
     */
    public @Blocking boolean save(RogueSmpCore plugin, String id) {
        T value = get(id);
        if (value == null) {
            RogueSmpCore.LOGGER.warn("Cannot save unknown entry '{}' in registry '{}'", id, locationKey);
            return false;
        }

        return saveEntry(plugin, id, value);
    }

    /**
     * Registers and immediately saves an entry to disk as a .json file.
     *
     * @param plugin The plugin instance
     * @param id     The registry key
     * @param value  The object to register and save
     * @return The registered value
     */
    public @Blocking <U extends T> U registerAndSave(RogueSmpCore plugin, String id, U value) {
        register(id, value);
        saveEntry(plugin, id, value);
        return value;
    }

    private boolean saveEntry(RogueSmpCore plugin, String id, T value) {
        if (locationKey == null || codec == null) {
            RogueSmpCore.LOGGER.warn("Attempted to save to an in-memory registry '{}'", id);
            return false;
        }

        File folder = new File(plugin.getDataFolder(), locationKey);
        if (!folder.exists() && !folder.mkdirs()) {
            RogueSmpCore.LOGGER.error("Failed to create directory structure for '{}'", locationKey);
            return false;
        }

        // Encode object -> JsonElement using Codec
        DataResult<JsonElement> result = codec.encode(value, JsonOps.INSTANCE);
        if (!result.isSuccess()) {
            RogueSmpCore.LOGGER.error("Failed to encode [{}] in '{}': {}", id, locationKey, result.error());
            return false;
        }

        File file = new File(folder, id + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            Utils.GSON.toJson(result.result(), writer);
            RogueSmpCore.LOGGER.info("Saved entry '{}' to '{}/{}.json'", id, locationKey, id);
            return true;
        } catch (Exception e) {
            RogueSmpCore.LOGGER.error("Error writing file '{}.json' in '{}': {}", id, locationKey, e.getMessage());
            return false;
        }
    }

    public <U extends T> U register(String id, U value) {
        entries.put(id, value);
        return value;
    }

    public @Nullable T get(String id) {
        return entries.get(id);
    }

    public T getOrThrow(String id) {
        T value = get(id);
        if (value == null) throw new IllegalArgumentException("Unknown key: " + id + " in " + locationKey);
        return value;
    }

    public @Unmodifiable Map<String, T> getAll() {
        return unmodifiableEntries;
    }

    public @Nullable SmpTag<T> getTag(String id) {
        return tags.get(id.toLowerCase());
    }

    public @Unmodifiable Map<String, SmpTag<T>> getTags() {
        return unmodifiableTags;
    }

    public void clear() {
        entries.clear();
    }
}
