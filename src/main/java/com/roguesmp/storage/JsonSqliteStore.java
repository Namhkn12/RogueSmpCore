package com.roguesmp.storage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.JsonOps;
import com.roguesmp.utils.Utils;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A single-table SQLite store that keeps one codec-encoded JSON document per UUID.
 * Every database operation runs on one dedicated thread, in submission order, so a read always sees earlier writes
 * and {@link #close()} finishes every queued write before the connection is closed.
 * {@link #encode} touches no database state: call it on the thread that owns the value, then hand the string to a write.
 */
public class JsonSqliteStore<V> {

    private static final long CLOSE_TIMEOUT_SECONDS = 30;

    private final String table;
    private final String keyColumn;
    private final Codec<V> codec;
    private final Connection connection;
    private final ExecutorService executor;

    public JsonSqliteStore(RogueSmpCore plugin, String dbFileName, String table, String keyColumn, Codec<V> codec) {
        this.table = table;
        this.keyColumn = keyColumn;
        this.codec = codec;
        this.executor = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, table + "-sqlite"));

        File dbFile = new File(plugin.getDataFolder(), dbFileName);
        dbFile.getParentFile().mkdirs();

        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open " + table + " database", e);
        }

        String createSql = "CREATE TABLE IF NOT EXISTS " + table + " (" + keyColumn + " TEXT PRIMARY KEY NOT NULL, data BLOB NOT NULL)";
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA synchronous=NORMAL");
            statement.execute(createSql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create " + table + " table", e);
        }
    }

    public @Nullable String encode(V value) {
        DataResult<JsonElement> encoded = codec.encode(value, JsonOps.INSTANCE);
        if (!encoded.isSuccess()) {
            RogueSmpCore.LOGGER.error("Failed to encode {} data: {}", table, encoded.error());
            return null;
        }
        return Utils.GSON.toJson(encoded.result());
    }

    /**
     * @return a future that completes with whether the write succeeded
     */
    public CompletableFuture<Boolean> writeAsync(UUID key, String json) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO " + table + " (" + keyColumn + ", data) VALUES (?, jsonb(?)) "
                    + "ON CONFLICT(" + keyColumn + ") DO UPDATE SET data = excluded.data";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, key.toString());
                statement.setString(2, json);
                statement.executeUpdate();
                return true;
            } catch (SQLException e) {
                RogueSmpCore.LOGGER.error("Failed to save {} data (key: {})", table, key, e);
                return false;
            }
        }, executor);
    }

    /**
     * Writes all rows in one transaction, so the whole batch costs a single disk sync. Either every row is written or none is.
     */
    public CompletableFuture<Boolean> writeAllAsync(Map<UUID, String> rows) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO " + table + " (" + keyColumn + ", data) VALUES (?, jsonb(?)) "
                    + "ON CONFLICT(" + keyColumn + ") DO UPDATE SET data = excluded.data";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                connection.setAutoCommit(false);
                for (Map.Entry<UUID, String> row : rows.entrySet()) {
                    statement.setString(1, row.getKey().toString());
                    statement.setString(2, row.getValue());
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
                return true;
            } catch (SQLException e) {
                RogueSmpCore.LOGGER.error("Failed to save {} batch of {} row(s)", table, rows.size(), e);
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    RogueSmpCore.LOGGER.error("Failed to roll back {} batch", table, rollbackError);
                }
                return false;
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException e) {
                    RogueSmpCore.LOGGER.error("Failed to restore autocommit on {}", table, e);
                }
            }
        }, executor);
    }

    public @Blocking boolean write(UUID key, String json) {
        return writeAsync(key, json).join();
    }

    public @Blocking @Nullable V load(UUID key) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT json(data) AS data FROM " + table + " WHERE " + keyColumn + " = ?";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, key.toString());

                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? decode(key, result.getString("data")) : null;
                }
            } catch (SQLException e) {
                RogueSmpCore.LOGGER.error("Failed to load {} data (key: {})", table, key, e);
                return null;
            }
        }, executor).join();
    }

    public @Blocking Map<UUID, V> loadAll() {
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, V> all = new HashMap<>();
            String sql = "SELECT " + keyColumn + ", json(data) AS data FROM " + table;

            try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
                while (result.next()) {
                    UUID key = UUID.fromString(result.getString(keyColumn));
                    V value = decode(key, result.getString("data"));
                    if (value != null) all.put(key, value);
                }
            } catch (SQLException e) {
                RogueSmpCore.LOGGER.error("Failed to scan {} data", table, e);
            }
            return all;
        }, executor).join();
    }

    public @Blocking void delete(UUID key) {
        CompletableFuture.runAsync(() -> {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM " + table + " WHERE " + keyColumn + " = ?")) {
                statement.setString(1, key.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                RogueSmpCore.LOGGER.error("Failed to delete {} data (key: {})", table, key, e);
            }
        }, executor).join();
    }

    /**
     * Waits for every queued operation to finish, then closes the connection.
     */
    public @Blocking void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(CLOSE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                RogueSmpCore.LOGGER.error("Timed out waiting for pending {} writes", table);
            }
            connection.close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (SQLException e) {
            RogueSmpCore.LOGGER.error("Failed to close {} database", table, e);
        }
    }

    private @Nullable V decode(UUID key, String rawJson) {
        JsonObject json = Utils.GSON.fromJson(rawJson, JsonObject.class);
        if (json == null) return null;

        DataResult<V> decoded = codec.decode(json, JsonOps.INSTANCE);
        if (!decoded.isSuccess()) {
            RogueSmpCore.LOGGER.warn("Failed to decode {} data (key: {}): {}", table, key, decoded.error());
            return null;
        }
        return decoded.result();
    }
}
