package com.roguesmp.block.persistence;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.block.BlockPos;
import com.roguesmp.utils.Utils;

import java.nio.file.Path;
import java.sql.*;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Storage for custom block data
 */
public class BlockDataRepository {

    private static final String UPSERT = "INSERT INTO block_data(world, x, y, z, data) VALUES(?, ?, ?, ?, jsonb(?)) "
            + "ON CONFLICT(world, x, y, z) DO UPDATE SET data = excluded.data";

    private final Connection connection;
    private final Object lock = new Object();

    public BlockDataRepository(Path databaseFile) {

        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.toAbsolutePath());
            initialize();
        } catch (ClassNotFoundException | SQLException e) {
            throw new IllegalStateException("Failed to open block database", e);
        }

        RogueSmpCore.LOGGER.info("Block database ready");
    }

    public CompletableFuture<Optional<String>> load(BlockPos pos) {
        CompletableFuture<Optional<String>> future = new CompletableFuture<>();
        Utils.runAsync(() -> {
            try {
                future.complete(query(pos));
            } catch (SQLException e) {
                future.completeExceptionally(new IllegalStateException("Failed to load block state at " + pos, e));
            }
        });
        return future;
    }

    public void save(BlockPos pos, String json) {
        saveAll(Map.of(pos, json));
    }

    public void saveAll(Map<BlockPos, String> states) {
        if (states.isEmpty()) return;
        Utils.runAsync(() -> writeAll(states));
    }

    public void delete(BlockPos pos) {
        Utils.runAsync(() -> {
            synchronized (lock) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM block_data WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
                    bindKey(statement, pos);
                    statement.executeUpdate();
                } catch (SQLException e) {
                    RogueSmpCore.LOGGER.error("Failed to delete block state at {}", pos, e);
                }
            }
        });
    }

    /**
     * Writes {@code finalStates} on the calling thread and closes the connection. Bukkit cancels a
     * plugin's queued tasks when it disables, so anything still queued is covered by the caller
     * passing the complete current state here.
     */
    public void closeWith(Map<BlockPos, String> finalStates) {
        synchronized (lock) {
            if (!finalStates.isEmpty()) writeAll(finalStates);

            try {
                connection.close();
            } catch (SQLException e) {
                RogueSmpCore.LOGGER.error("Failed to close block database", e);
            }
        }
    }

    private Optional<String> query(BlockPos pos) throws SQLException {
        synchronized (lock) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT json(data) FROM block_data WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
                bindKey(statement, pos);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? Optional.of(rows.getString(1)) : Optional.empty();
                }
            }
        }
    }

    private void writeAll(Map<BlockPos, String> states) {
        synchronized (lock) {
            try {
                connection.setAutoCommit(false);
                try (PreparedStatement statement = connection.prepareStatement(UPSERT)) {
                    for (Map.Entry<BlockPos, String> entry : states.entrySet()) {
                        bindKey(statement, entry.getKey());
                        statement.setString(5, entry.getValue());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
                connection.commit();
            } catch (SQLException e) {
                RogueSmpCore.LOGGER.error("Failed to save {} block state(s)", states.size(), e);
                rollbackQuietly();
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException e) {
                    RogueSmpCore.LOGGER.error("Failed to restore auto-commit on block database", e);
                }
            }
        }
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException e) {
            RogueSmpCore.LOGGER.error("Failed to roll back block database transaction", e);
        }
    }

    private void bindKey(PreparedStatement statement, BlockPos pos) throws SQLException {
        statement.setString(1, pos.world().toString());
        statement.setInt(2, pos.x());
        statement.setInt(3, pos.y());
        statement.setInt(4, pos.z());
    }

    private void initialize() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA synchronous = NORMAL");
            statement.execute("CREATE TABLE IF NOT EXISTS block_data("
                    + "world TEXT NOT NULL, x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL, "
                    + "data BLOB NOT NULL, PRIMARY KEY(world, x, y, z)) WITHOUT ROWID");
        }
    }
}
