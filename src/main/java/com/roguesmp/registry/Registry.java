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
import java.util.function.Supplier;

public class Registry<T> {

    private static final Codec<List<String>> TAG_CODEC = Codec.listOf(Codec.STRING);

    private final Map<String, T> entries = new HashMap<>();
    private final Map<String, T> unmodifiableEntries = Collections.unmodifiableMap(entries);
    private final Map<String, SmpTag<T>> tags = new HashMap<>();
    private final Map<String, SmpTag<T>> unmodifiableTags = Collections.unmodifiableMap(tags);
    private final Map<String, Holder<T>> holders = new HashMap<>();
    private final String locationKey; // Subfolder name inside data folder (e.g., "items", "recipes")
    private final Codec<T> codec;

    private static final List<Registry<?>> DATA_REGISTRIES = new ArrayList<>();
    private static final List<Registry<?>> ALL_REGISTRIES = new ArrayList<>();

    // Constructor for Data-Driven Registries
    public Registry(String locationKey, Codec<T> codec) {
        this.locationKey = locationKey;
        this.codec = codec;
        DATA_REGISTRIES.add(this);
        ALL_REGISTRIES.add(this);
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
        ALL_REGISTRIES.add(this);
    }

    // Constructor for In-Memory Only Registries
    public Registry() {
        this.locationKey = null;
        this.codec = null;
        ALL_REGISTRIES.add(this);
    }

    /**
     * Loads every {@code .json} file under this registry's locationKey folder, recursing into
     * subfolders. The registry id for each file is its path relative to that folder with the
     * extension stripped, so nesting is reflected directly in the id and the id always points
     * straight back at its own file - e.g. {@code items/weapons/fire_sword.json} -> id
     * {@code "weapons/fire_sword"}. A flat folder (no subfolders) behaves exactly as before:
     * relative path of a top-level file is just its own filename.
     */
    public @Blocking void loadFrom(RogueSmpCore plugin) {
        if (locationKey == null || codec == null) return;

        File folder = new File(plugin.getDataFolder(), locationKey);
        if (!folder.exists()) {
            folder.mkdirs();
            return;
        }

        RogueSmpCore.LOGGER.info("Loading entries for registry '{}'", locationKey);
        int[] count = {0};
        loadFromRecursive(folder, folder, count);
        RogueSmpCore.LOGGER.info("Loaded {} entries into registry '{}'", count[0], locationKey);
    }

    /**
     * @param root current registry's data folder - ids are derived relative to this
     * @param dir  folder currently being walked - {@code root} on the initial call, a subfolder
     *             on recursive calls
     */
    private void loadFromRecursive(File root, File dir, int[] count) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                // "tags" is reserved for loadTagsFrom - never walk into it as regular entries,
                // or every tag file (a bare JSON array) would also be attempted as an entry and
                // fail to decode.
                if (file.getName().equalsIgnoreCase("tags")) continue;
                loadFromRecursive(root, file, count);
                continue;
            }
            if (!file.getName().toLowerCase().endsWith(".json")) continue;

            String id = relativeId(root, file);

            try (FileReader reader = new FileReader(file)) {
                JsonElement json = Utils.GSON.fromJson(reader, JsonElement.class);
                if (json == null) continue;

                DataResult<T> result = codec.decode(json, JsonOps.INSTANCE);
                if (result.isSuccess()) {
                    register(id, result.result());
                    count[0]++;
                } else {
                    RogueSmpCore.LOGGER.error("Failed to decode [{}] in '{}': {}", id, locationKey, result.error());
                }
            } catch (Exception e) {
                RogueSmpCore.LOGGER.error("Error reading file '{}' in '{}': {}", file.getName(), locationKey, e.getMessage());
            }
        }
    }

    /**
     * File path relative to {@code root}, extension stripped - e.g. {@code weapons/fire_sword}.
     */
    private String relativeId(File root, File file) {
        String relative = root.toURI().relativize(file.toURI()).getPath();
        return relative.replaceAll("\\.json$", "");
    }

    /**
     * Loads ALL data-driven registries from plugin data folder
     */
    public static void loadAll(RogueSmpCore plugin) {
        RogueSmpCore.LOGGER.info("----LOADING REGISTRY ENTRIES----");
        for (Registry<?> registry : DATA_REGISTRIES) {
            registry.loadFrom(plugin);
        }
        RogueSmpCore.LOGGER.info("----DONE LOADING REGISTRY----");
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

        RogueSmpCore.LOGGER.info("Loading tags for registry '{}'", locationKey);
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
        RogueSmpCore.LOGGER.info("----LOADING REGISTRY TAG ENTRIES----");
        for (Registry<?> registry : DATA_REGISTRIES) {
            registry.loadTagsFrom(plugin);
        }
        RogueSmpCore.LOGGER.info("----DONE LOADING TAG----");
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
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            RogueSmpCore.LOGGER.error("Failed to create directory structure for '{}'", parent.getPath());
            return false;
        }

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
        Holder<T> holder = holders.get(id);
        if (holder != null) holder.bind(value);
        return value;
    }

    /**
     * Removes an entry from memory and deletes its associated {@code .json} file from disk.
     * Unbinds any existing {@link Holder} associated with this ID. See {@link #unregister(String)} for remove from memory only.
     *
     * @param plugin The plugin instance (used to locate the data folder)
     * @param id     The registry key/filename (without .json extension)
     * @return true if the entry was removed from memory and the file was successfully deleted (or didn't exist)
     */
    public @Blocking boolean removeAndDeleteFiles(RogueSmpCore plugin, String id) {
        if (locationKey == null) {
            RogueSmpCore.LOGGER.warn("Attempted to delete file for in-memory registry '{}'", "<in-memory>");
            return unregister(id);
        }

        // 1. Remove from in-memory maps
        boolean wasRegistered = unregister(id);

        // 2. Delete file from disk
        File folder = new File(plugin.getDataFolder(), locationKey);
        File file = new File(folder, id + ".json");

        if (!file.exists()) {
            RogueSmpCore.LOGGER.warn("File '{}.json' in '{}' did not exist on disk during deletion", id, locationKey);
            return wasRegistered;
        }

        boolean deleted = file.delete();
        if (deleted) {
            RogueSmpCore.LOGGER.info("Deleted entry file '{}/{}.json'", locationKey, id);
        } else {
            RogueSmpCore.LOGGER.error("Failed to delete file '{}/{}.json'", locationKey, id);
        }

        return deleted;
    }

    /**
     * Removes an entry from in-memory maps and unbinds its {@link Holder}.
     *
     * @param id The registry key to unregister
     * @return true if an entry was previously registered under this ID
     */
    public boolean unregister(String id) {
        T removed = entries.remove(id);
        Holder<T> holder = holders.get(id);
        if (holder != null) {
            holder.unbind();
        }
        return removed != null;
    }

    public @Nullable T get(String id) {
        return entries.get(id);
    }

    /**
     * Returns a stable {@link Holder} reference for this id, whether or not anything is
     * registered under it yet - see {@link Holder} for why that's useful (load-order independence,
     * reload safety). Repeated calls with the same id return the exact same Holder instance.
     */
    public Holder<T> getHolder(String id) {
        return holders.computeIfAbsent(id, key -> {
            T existing = entries.get(key);
            return existing != null ? new Holder<>(key, existing) : new Holder<>(key);
        });
    }

    /**
     * A ready-made codec for referencing an entry of some registry by id as a {@link Holder} - so
     * you don't need to hand-write {@code Codec.STRING.xmap(registry::getHolder, Holder::getId)}
     * for every registry that needs one.
     * <p>
     * Takes a <b>supplier</b> (e.g. {@code () -> Registries.QUEST}) rather than a {@code Registry<T>}
     * directly, and never calls it until this codec is actually decoded/encoded. That's not
     * incidental: a type whose own static field references {@code Registries.SOMETHING} can end up
     * loaded *during* {@code Registries}'s own {@code <clinit>} (e.g. because constructing
     * {@code Registries.QUEST} itself needs {@code Quest.CODEC}, which forces {@code Quest} to load
     * first) - at that point {@code Registries.SOMETHING} may not be assigned yet, and reading it
     * eagerly throws a {@code NullPointerException}. Deferring the read into a supplier, invoked
     * only once real decoding happens (always well after every class has finished loading), makes
     * this safe to use anywhere, regardless of load order.
     */
    public static <T> Codec<Holder<T>> referenceCodec(Supplier<Registry<T>> registrySupplier) {
        return Codec.STRING.xmap(
                id -> registrySupplier.get().getHolder(id),
                Holder::getId
        );
    }

    /**
     * @return every id that's been asked for via {@link #getHolder(String)} but never registered -
     * i.e. a dangling reference. Empty in the common case.
     */
    public @Unmodifiable List<String> getUnboundHolderIds() {
        List<String> unresolved = new ArrayList<>();
        holders.forEach((id, holder) -> {
            if (!holder.isBound()) unresolved.add(id);
        });
        return List.copyOf(unresolved);
    }

    /**
     * Checks every registry (data-driven or in-memory) for holders nobody ever registered a value
     * for, and logs a warning for each. Call once, after all loading/bootstrapping finishes, to
     * catch a bad/typo'd reference id at startup instead of a {@link Holder#value()} exception
     * deep in gameplay code.
     */
    public static void validateAllHolders() {
        for (Registry<?> registry : ALL_REGISTRIES) {
            for (String id : registry.getUnboundHolderIds()) {
                RogueSmpCore.LOGGER.warn("Unresolved reference '{}' in registry '{}' - nothing is registered under this id",
                        id, registry.locationKey != null ? registry.locationKey : "<in-memory>");
            }
        }
    }

    public T getOrThrow(String id) {
        T value = get(id);
        if (value == null) throw new IllegalArgumentException("Unknown key: " + id + " in " + locationKey);
        return value;
    }

    public T getOrDefault(String id, T defaultVal) {
        T value = get(id);
        if (value == null) return defaultVal;
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
        // Unbind rather than drop: existing Holder references stay valid objects and simply pick
        // up their new value once this registry repopulates, instead of pointing at a stale entry.
        holders.values().forEach(Holder::unbind);
    }

    /**
     * Clears every data-driven registry's entries - call immediately before {@link #loadAll} to
     * reload from disk without leaving stale entries for a file removed since the last load.
     */
    public static void clearAll() {
        for (Registry<?> registry : DATA_REGISTRIES) {
            registry.clear();
        }
    }

    public @Nullable String getLocationKey() {
        return locationKey;
    }

    /**
     * Location key of every registry that's actually file-backed (has a codec) - e.g. "items",
     * "entities". Excludes code-populated registries (e.g. {@code enchants}) whose entries come
     * from a bootstrapper, not a {@code *.json} folder, and so would just be wiped by a reload
     * rather than repopulated.
     */
    public static @Unmodifiable List<String> getReloadableKeys() {
        List<String> keys = new ArrayList<>();
        for (Registry<?> registry : DATA_REGISTRIES) {
            if (registry.locationKey != null && registry.codec != null) keys.add(registry.locationKey);
        }
        return List.copyOf(keys);
    }

    /**
     * Clears and reloads (entries + tags) the single data-driven registry matching
     * {@code locationKey} (case-insensitive). Returns {@code false} if no reloadable registry has
     * that key.
     */
    public static boolean reloadOne(String locationKey, RogueSmpCore plugin) {
        for (Registry<?> registry : DATA_REGISTRIES) {
            if (registry.codec != null && locationKey.equalsIgnoreCase(registry.locationKey)) {
                registry.clear();
                registry.loadFrom(plugin);
                registry.loadTagsFrom(plugin);
                Registry.validateAllHolders();
                return true;
            }
        }
        return false;
    }
}
