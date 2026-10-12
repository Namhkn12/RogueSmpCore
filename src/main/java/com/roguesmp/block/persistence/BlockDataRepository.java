package com.roguesmp.block.persistence;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.block.BlockPos;

import org.bukkit.NamespacedKey;

import java.nio.file.Path;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Storage for custom block data. Rows are keyed by the world key (for example "minecraft:overworld").
 */
public class BlockDataRepository {

    private record Row(String world, int x, int y, int z) {
    }

    private static final String UPSERT = "INSERT INTO block_data(world, x, y, z, data) VALUES(?, ?, ?, ?, jsonb(?)) "
            + "ON CONFLICT(world, x, y, z) DO UPDATE SET data = excluded.data";

    private final Connection connection;
    private final Object lock = new Object();
    private final ExecutorService io = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "block-data-sqlite"));

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
        Row row = rowOf(pos);
        io.execute(() -> {
            try {
                future.complete(query(row));
            } catch (SQLException e) {
                future.completeExceptionally(new IllegalStateException("Failed to load block state at " + pos, e));
            }
        });
        return future;
    }

    public void saveAll(Map<BlockPos, String> states) {
        if (states.isEmpty()) return;
        Map<Row, String> rows = rowsOf(states);
        io.execute(() -> writeAll(rows));
    }

    /**
     * Writes {@code pending} first, then copies every row of {@code from} to {@code to}, in one task so the copy sees the pending states.
     */
    public CompletableFuture<Void> copyWorld(NamespacedKey from, NamespacedKey to, Map<BlockPos, String> pending) {
        Map<Row, String> pendingRows = rowsOf(pending);
        CompletableFuture<Void> future = new CompletableFuture<>();
        io.execute(() -> {
            synchronized (lock) {
                try {
                    if (!pendingRows.isEmpty()) writeAll(pendingRows);
                    try (PreparedStatement statement = connection.prepareStatement(
                            "INSERT OR REPLACE INTO block_data(world, x, y, z, data) SELECT ?, x, y, z, data FROM block_data WHERE world = ?")) {
                        statement.setString(1, to.asString());
                        statement.setString(2, from.asString());
                        statement.executeUpdate();
                    }
                    future.complete(null);
                } catch (SQLException e) {
                    future.completeExceptionally(new IllegalStateException("Failed to copy block states of world " + from, e));
                }
            }
        });
        return future;
    }

    public void deleteWorld(NamespacedKey world) {
        io.execute(() -> {
            synchronized (lock) {
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM block_data WHERE world = ?")) {
                    statement.setString(1, world.asString());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    RogueSmpCore.LOGGER.error("Failed to delete block states of world {}", world, e);
                }
            }
        });
    }

    public void delete(BlockPos pos) {
        Row row = rowOf(pos);
        io.execute(() -> {
            synchronized (lock) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM block_data WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
                    bindKey(statement, row);
                    statement.executeUpdate();
                } catch (SQLException e) {
                    RogueSmpCore.LOGGER.error("Failed to delete block state at {}", row, e);
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
        io.shutdown();
        try {
            io.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Map<Row, String> rows = rowsOf(finalStates);
        synchronized (lock) {
            if (!rows.isEmpty()) writeAll(rows);

            try {
                connection.close();
            } catch (SQLException e) {
                RogueSmpCore.LOGGER.error("Failed to close block database", e);
            }
        }
    }

    private Optional<String> query(Row row) throws SQLException {
        synchronized (lock) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT json(data) FROM block_data WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
                bindKey(statement, row);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? Optional.of(rows.getString(1)) : Optional.empty();
                }
            }
        }
    }

    private void writeAll(Map<Row, String> states) {
        synchronized (lock) {
            try {
                connection.setAutoCommit(false);
                try (PreparedStatement statement = connection.prepareStatement(UPSERT)) {
                    for (Map.Entry<Row, String> entry : states.entrySet()) {
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

    private void bindKey(PreparedStatement statement, Row row) throws SQLException {
        statement.setString(1, row.world());
        statement.setInt(2, row.x());
        statement.setInt(3, row.y());
        statement.setInt(4, row.z());
    }
    private static Row rowOf(BlockPos pos) {
        return new Row(pos.world().asString(), pos.x(), pos.y(), pos.z());
    }

    private static Map<Row, String> rowsOf(Map<BlockPos, String> states) {
        Map<Row, String> rows = new HashMap<>();
        states.forEach((pos, json) -> rows.put(rowOf(pos), json));
        return rows;
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
