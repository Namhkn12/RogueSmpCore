package com.roguesmp.island;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.utils.Utils;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handle saving/loading island data to/from a global SQLite database
 * (replaces the old one-json-file-per-island storage, which had no
 * locking against concurrent saves from multiple online island members).
 */
public class IslandDataManager {

    private static final String DB_FILE_NAME = "island_data.db";

    private final Map<UUID, IslandData> islandDataCache = new HashMap<>();
    private final Connection connection;

    public IslandDataManager(RogueSmpCore plugin) {
        this.connection = openConnection(plugin);
        createTable();
    }

    private Connection openConnection(RogueSmpCore plugin) {
        File dbFile = new File(plugin.getDataFolder(), DB_FILE_NAME);
        File parent = dbFile.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        try {
            return DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open island data database", e);
        }
    }

    private void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS island_data (
                    island_id TEXT PRIMARY KEY NOT NULL,
                    data BLOB NOT NULL
                )
                """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create island_data table", e);
        }
    }

    public @Blocking synchronized void saveIslandData(IslandData islandData) {
        if (islandData == null) return;
        IslandData clone = new IslandData(islandData);
        RogueSmpCore.LOGGER.info("Saving island data with ID: {}", clone.getIslandId());

        String sql = """
                INSERT INTO island_data (island_id, data)
                VALUES (?, jsonb(?))
                ON CONFLICT(island_id) DO UPDATE SET data = excluded.data
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, clone.getIslandId().toString());
            statement.setString(2, Utils.GSON.toJson(clone));
            statement.executeUpdate();
            islandData.setDirty(false);
            RogueSmpCore.LOGGER.info("Saved island data with ID: {}", clone.getIslandId());
        } catch (SQLException e) {
            RogueSmpCore.LOGGER.error("Failed to save island data for ID: {}", clone.getIslandId(), e);
        }
    }

    public @Nullable @Blocking synchronized IslandData loadIslandData(UUID islandId) {
        if (islandId == null) return null;
        RogueSmpCore.LOGGER.info("Loading island data with ID: {}", islandId);
        if (islandDataCache.containsKey(islandId)) {
            RogueSmpCore.LOGGER.info("Island data with ID {} is already cached.", islandId);
            return islandDataCache.get(islandId);
        }

        String sql = "SELECT json(data) AS data FROM island_data WHERE island_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, islandId.toString());

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }

                IslandData islandData = Utils.GSON.fromJson(result.getString("data"), IslandData.class);
                RogueSmpCore.LOGGER.info("Loaded island data with ID: {}", islandId);
                return islandData;
            }
        } catch (SQLException e) {
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
        close();
    }

    public void saveAllIslandData() {
        if (islandDataCache.isEmpty()) {
            return;
        }

        RogueSmpCore.LOGGER.info("Saving {} active coop islands...", islandDataCache.size());

        for (IslandData islandData : islandDataCache.values()) {
            saveIslandData(islandData);
        }

        RogueSmpCore.LOGGER.info("All island data has been saved.");
    }

    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            RogueSmpCore.LOGGER.error("Failed to close island data database", e);
        }
    }
}
