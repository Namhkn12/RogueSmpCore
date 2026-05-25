package com.roguesmp.island;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.utils.Utils;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IslandDataManager {

    public static final String FOLDER_NAME = "sb_island_data";

    private final RogueSmpCore plugin;
    private final Map<UUID, IslandData> islandDataCache = new HashMap<>();

    public IslandDataManager(RogueSmpCore plugin) {
        this.plugin = plugin;
    }

    public @Blocking void saveIslandData(IslandData islandData) {
        if (islandData == null) return;
        String json = Utils.GSON.toJson(islandData); //Basically a snapshot
        RogueSmpCore.LOGGER.info("Saving island data with ID: {}", islandData.getIslandId());
        File dataFolder = new File(plugin.getDataFolder(), FOLDER_NAME);
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File file = new File(dataFolder, islandData.getIslandId().toString() + ".json");

        try (FileWriter writer = new FileWriter(file)) {
            Utils.GSON.toJson(json, writer);
            RogueSmpCore.LOGGER.info("Saved island data with ID: {}", islandData.getIslandId());
        } catch (Exception e) {
            RogueSmpCore.LOGGER.error("Failed to save island data for ID: {}", islandData.getIslandId(), e);
        }
    }

    public @Nullable @Blocking IslandData loadIslandData(UUID islandId) {
        if (islandId == null) return null;
        RogueSmpCore.LOGGER.info("Loading island data with ID: {}", islandId);
        if (islandDataCache.containsKey(islandId)) {
            RogueSmpCore.LOGGER.info("Island data with ID {} is already cached.", islandId);
            return islandDataCache.get(islandId);
        }
        File dataFolder = new File(plugin.getDataFolder(), FOLDER_NAME);
        File file = new File(dataFolder, islandId + ".json");
        IslandData islandData;
        try (FileReader reader = new FileReader(file)) {
            islandData = Utils.GSON.fromJson(reader, IslandData.class);
            RogueSmpCore.LOGGER.info("Loaded island data with ID: {}", islandId);
            return islandData;
        } catch (Exception e) {
            RogueSmpCore.LOGGER.error("Failed to load island data with ID: {}", islandId, e);
        }
        return null;
    }

    public @Nullable IslandData getCachedData(UUID islandId) {
        return islandDataCache.get(islandId);
    }

    public @Unmodifiable Map<UUID, IslandData> getIslandDataCache() {
        return Collections.unmodifiableMap(islandDataCache);
    }

    public @Nullable IslandData removeCache(UUID uuid) {
        return islandDataCache.remove(uuid);
    }

    /**
     * Cache this data if it's not already cached
     */
    public void cache(IslandData islandData) {
        islandDataCache.putIfAbsent(islandData.getIslandId(), islandData);
    }

    public void onDisable() {
        saveAllIslandData();
    }

    public void saveAllIslandData() {
        if (islandDataCache.isEmpty()) {
            return;
        }

        RogueSmpCore.LOGGER.info("Saving {} active coop islands...", islandDataCache.size());

        for (IslandData islandData : islandDataCache.values()) {
            saveIslandData(islandData);
        }

        RogueSmpCore.LOGGER.info("All island data files have been written successfully.");
    }
}
