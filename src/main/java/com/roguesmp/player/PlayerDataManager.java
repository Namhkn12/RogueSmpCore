package com.roguesmp.player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.JsonOps;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handle saving/loading player data to/from a single global SQLite database
 * (replaces the old one-json-file-per-player storage).
 */
public class PlayerDataManager {
    private static final String DB_FILE_NAME = "player_data.db";

    private final Map<UUID, PlayerData> playerDataCache = new HashMap<>();
    private final Connection connection;

    public PlayerDataManager() {
        this.connection = openConnection();
        createTable();
    }

    private Connection openConnection() {
        File dbFile = new File(RogueSmpCore.getInstance().getDataFolder(), DB_FILE_NAME);
        File parent = dbFile.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        try {
            return DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open player data database", e);
        }
    }

    private void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS player_data (
                    uuid TEXT PRIMARY KEY NOT NULL,
                    data BLOB NOT NULL
                )
                """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create player_data table", e);
        }
    }

    public void cacheData(PlayerData data) {
        playerDataCache.put(data.getUuid(), data);
    }

    public PlayerData removeCachedData(UUID uuid) {
        return playerDataCache.remove(uuid);
    }

    public @Blocking synchronized void savePlayerData(PlayerData playerData) {
        if (playerData == null) return;

        DataResult<JsonElement> encoded = PlayerData.CODEC.encode(playerData, JsonOps.INSTANCE);
        if (!encoded.isSuccess()) {
            RogueSmpCore.LOGGER.error("Failed to encode player data (uuid: {}): {}", playerData.getUuid(), encoded.error());
            return;
        }

        String sql = """
                INSERT INTO player_data (uuid, data)
                VALUES (?, jsonb(?))
                ON CONFLICT(uuid) DO UPDATE SET data = excluded.data
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerData.getUuid().toString());
            statement.setString(2, Utils.GSON.toJson(encoded.result()));
            statement.executeUpdate();

            playerData.setDirty(false);
            RogueSmpCore.LOGGER.info("Player data saved (uuid: {})", playerData.getUuid());
        } catch (SQLException e) {
            RogueSmpCore.LOGGER.error("Failed to save player data (uuid: {})", playerData.getUuid(), e);
        }
    }

    public @Blocking synchronized PlayerData loadPlayerData(UUID uuid) {
        RogueSmpCore.LOGGER.info("Loading player data (uuid: {})", uuid);

        String sql = "SELECT json(data) AS data FROM player_data WHERE uuid = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    RogueSmpCore.LOGGER.info("Player data not found, creating default");
                    return createDefault(uuid);
                }

                JsonObject json = Utils.GSON.fromJson(result.getString("data"), JsonObject.class);
                if (json == null) return createDefault(uuid);

                DataResult<PlayerData> decoded = PlayerData.CODEC.decode(json, JsonOps.INSTANCE);
                if (!decoded.isSuccess()) {
                    RogueSmpCore.LOGGER.warn("Failed to decode player data for {}: {}", uuid, decoded.error());
                    return createDefault(uuid);
                }

                RogueSmpCore.LOGGER.info("Loaded player data (uuid: {})", uuid);
                return decoded.result();
            }
        } catch (SQLException e) {
            RogueSmpCore.LOGGER.error("Failed to load player data (uuid: {})", uuid, e);
            return createDefault(uuid);
        }
    }

    public @Nullable PlayerData getData(UUID uuid) {
        return playerDataCache.get(uuid);
    }

    public @Unmodifiable Collection<PlayerData> getCachedPlayerData() {
        return Collections.unmodifiableCollection(playerDataCache.values());
    }

    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            RogueSmpCore.LOGGER.error("Failed to close player data database", e);
        }
    }

    private static PlayerData createDefault(UUID uuid) {
        return new PlayerData(uuid);
    }
}
