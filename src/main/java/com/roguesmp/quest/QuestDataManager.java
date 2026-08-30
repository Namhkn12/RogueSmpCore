package com.roguesmp.quest;

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
 * Handle saving/loading player quest data to/from a global SQLite database
 * (replaces the old one-json-file-per-player storage, which raced between
 * the async save on quit and the synchronous save from claimAndReplenish).
 */
public class QuestDataManager {

    private static final String DB_FILE_NAME = "player_quest_data.db";

    private final Map<UUID, PlayerQuestData> dataMap = new HashMap<>();
    private final Connection connection;

    public QuestDataManager(RogueSmpCore plugin) {
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
            throw new RuntimeException("Failed to open quest data database", e);
        }
    }

    private void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS player_quest_data (
                    uuid TEXT PRIMARY KEY NOT NULL,
                    data BLOB NOT NULL
                )
                """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create player_quest_data table", e);
        }
    }

    public @Blocking synchronized PlayerQuestData loadData(UUID uuid) {
        RogueSmpCore.LOGGER.info("Loading quest data for uuid {}", uuid);

        String sql = "SELECT json(data) AS data FROM player_quest_data WHERE uuid = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return createDefault(uuid);
                }

                JsonObject json = Utils.GSON.fromJson(result.getString("data"), JsonObject.class);
                if (json == null) return createDefault(uuid);

                DataResult<PlayerQuestData> decoded = PlayerQuestData.CODEC.decode(json, JsonOps.INSTANCE);
                if (!decoded.isSuccess()) {
                    RogueSmpCore.LOGGER.warn("Failed to decode quest data for {}: {}", uuid, decoded.error());
                    return createDefault(uuid);
                }
                return decoded.result();
            }
        } catch (SQLException e) {
            RogueSmpCore.LOGGER.error("Failed to load quest data for uuid {}", uuid, e);
            return createDefault(uuid);
        }
    }

    public @Blocking synchronized void saveData(PlayerQuestData playerQuestData) {
        PlayerQuestData data = new PlayerQuestData(playerQuestData);
        RogueSmpCore.LOGGER.info("Saving player quest data (uuid: {})", playerQuestData.getUuid());

        DataResult<JsonElement> encoded = PlayerQuestData.CODEC.encode(data, JsonOps.INSTANCE);
        if (!encoded.isSuccess()) {
            RogueSmpCore.LOGGER.error(encoded.error());
            return;
        }

        String sql = """
                INSERT INTO player_quest_data (uuid, data)
                VALUES (?, jsonb(?))
                ON CONFLICT(uuid) DO UPDATE SET data = excluded.data
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, data.getUuid().toString());
            statement.setString(2, Utils.GSON.toJson(encoded.result()));
            statement.executeUpdate();
            playerQuestData.setDirty(false);
        } catch (SQLException e) {
            RogueSmpCore.LOGGER.warn("Failed to save player quest data (uuid: {})", playerQuestData.getUuid());
        }
    }

    public @Blocking void saveAllData() {
        dataMap.forEach((uuid, playerQuestData) -> saveData(playerQuestData));
    }

    public @Nullable PlayerQuestData getCachedData(UUID uuid) {
        return dataMap.get(uuid);
    }

    public @Unmodifiable Collection<PlayerQuestData> getCachedQuestData() {
        return Collections.unmodifiableCollection(dataMap.values());
    }

    public void cache(PlayerQuestData playerQuestData) {
        dataMap.put(playerQuestData.getUuid(), playerQuestData);
    }

    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            RogueSmpCore.LOGGER.error("Failed to close quest data database", e);
        }
    }

    private PlayerQuestData createDefault(UUID uuid) {
        return new PlayerQuestData(uuid);
    }
}
