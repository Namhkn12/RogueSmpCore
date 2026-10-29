package com.roguesmp.quest.data;

import com.roguesmp.quest.holder.GroupHolder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.storage.JsonSqliteStore;
import com.roguesmp.storage.JsonSqliteStore.SqlStatement;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class QuestDataManager {

    private final Map<UUID, PlayerQuestData> dataMap = new ConcurrentHashMap<>();
    private final JsonSqliteStore<PlayerQuestData> store;
    private final Map<UUID, GroupQuestData> groupMap = new HashMap<>();
    private final JsonSqliteStore<GroupQuestData> groupStore;
    private final Executor mainThread;

    public QuestDataManager(RogueSmpCore plugin) {
        this.store = new JsonSqliteStore<>(plugin, "player_quest_data.db", "player_quest_data", "uuid", PlayerQuestData.CODEC);
        this.groupStore = new JsonSqliteStore<>(plugin, "group_quest_data.db", "group_quest_data", "group_id", GroupQuestData.CODEC);
        this.mainThread = Bukkit.getScheduler().getMainThreadExecutor(plugin);
        store.executeAsync(List.of(
                SqlStatement.of("CREATE TABLE IF NOT EXISTS " + RANKING_TABLE + " (uuid TEXT PRIMARY KEY NOT NULL, position INTEGER NOT NULL, exp INTEGER NOT NULL)"),
                SqlStatement.of("CREATE INDEX IF NOT EXISTS " + RANKING_TABLE + "_position ON " + RANKING_TABLE + " (position)")
        ));
    }

    public @Blocking PlayerQuestData loadData(UUID uuid) {
        PlayerQuestData loaded = store.load(uuid);
        return loaded != null ? loaded : new PlayerQuestData(uuid);
    }

    /**
     * Main thread only: the data is encoded immediately, then queued on the store in call order.
     */
    public void saveAsync(PlayerQuestData playerQuestData) {
        String json = store.encode(new PlayerQuestData(playerQuestData));
        if (json == null) return;

        playerQuestData.setDirty(false);
        store.writeAsync(playerQuestData.getUuid(), json).thenAccept(written -> {
            if (!written) playerQuestData.setDirty(true);
        });
    }

    public @Blocking void saveData(PlayerQuestData playerQuestData) {
        String json = store.encode(new PlayerQuestData(playerQuestData));
        if (json == null) return;

        if (store.write(playerQuestData.getUuid(), json)) playerQuestData.setDirty(false);
    }

    /**
     * Main thread only: the data is encoded immediately and all profiles are written in one transaction.
     */
    public CompletableFuture<Void> saveAllAsync(Collection<PlayerQuestData> profiles) {
        Map<UUID, String> rows = new HashMap<>();
        List<PlayerQuestData> encoded = new ArrayList<>();
        for (PlayerQuestData profile : profiles) {
            String json = store.encode(new PlayerQuestData(profile));
            if (json == null) continue;

            profile.setDirty(false);
            rows.put(profile.getUuid(), json);
            encoded.add(profile);
        }

        return store.writeAllAsync(rows).thenAccept(written -> {
            if (!written) encoded.forEach(profile -> profile.setDirty(true));
        });
    }

    public @Blocking void saveAllData() {
        dataMap.forEach((uuid, playerQuestData) -> saveData(playerQuestData));
        groupMap.values().forEach(this::saveGroupData);
    }

    /**
     * Loads the group's saved log, or starts a new one, and attaches the live members to it. Completes on the main thread.
     */
    public CompletableFuture<GroupQuestData> bindGroup(GroupHolder holder) {
        GroupQuestData cached = groupMap.get(holder.id());
        if (cached != null) {
            cached.bind(holder);
            return CompletableFuture.completedFuture(cached);
        }
        return groupStore.loadAsync(holder.id()).thenApplyAsync(loaded -> {
            GroupQuestData data = groupMap.computeIfAbsent(holder.id(), id -> loaded != null ? loaded : new GroupQuestData(holder));
            data.bind(holder);
            return data;
        }, mainThread);
    }

    public @Nullable GroupQuestData getCachedGroupData(UUID groupId) {
        return groupMap.get(groupId);
    }

    public @Unmodifiable Collection<GroupQuestData> getCachedGroupQuestData() {
        return Collections.unmodifiableCollection(groupMap.values());
    }

    public void saveGroupAsync(GroupQuestData data) {
        String json = groupStore.encode(new GroupQuestData(data));
        if (json == null) return;
        data.setDirty(false);
        groupStore.writeAsync(data.getHolder().id(), json).thenAccept(written -> {
            if (!written) data.setDirty(true);
        });
    }

    public CompletableFuture<Void> saveAllGroupsAsync(Collection<GroupQuestData> groups) {
        Map<UUID, String> rows = new HashMap<>();
        List<GroupQuestData> encoded = new ArrayList<>();
        for (GroupQuestData group : groups) {
            String json = groupStore.encode(new GroupQuestData(group));
            if (json == null) continue;
            group.setDirty(false);
            rows.put(group.getHolder().id(), json);
            encoded.add(group);
        }
        return groupStore.writeAllAsync(rows).thenAccept(written -> {
            if (!written) encoded.forEach(group -> group.setDirty(true));
        });
    }

    /**
     * Ends a live group and deletes it. Rewards were already delivered when each quest completed.
     */
    public void endGroup(UUID groupId) {
        if (groupMap.remove(groupId) != null) groupStore.deleteAsync(groupId);
    }

    /**
     * Startup only: ends every saved group of {@code owner} that is not in {@code liveGroupIds}, for owners whose groups
     * outlived them (a crash, or an owner removed while the server was down).
     *
     * @return how many groups were ended
     */
    @Blocking
    public int sweepGroups(String owner, Set<UUID> liveGroupIds) {
        int ended = 0;
        for (GroupQuestData data : groupStore.loadAll().values()) {
            UUID groupId = data.getHolder().id();
            if (!data.getHolder().owner().equals(owner) || liveGroupIds.contains(groupId) || groupMap.containsKey(groupId)) continue;
            groupStore.deleteAsync(groupId);
            ended++;
        }
        return ended;
    }

    @Blocking
    private void saveGroupData(GroupQuestData data) {
        String json = groupStore.encode(new GroupQuestData(data));
        if (json == null) return;
        if (groupStore.write(data.getHolder().id(), json)) data.setDirty(false);
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

    public void uncache(UUID uuid) {
        dataMap.remove(uuid);
    }

    /**
     * Saves the player's data and removes it from the cache. The save is encoded before removal, and later loads are
     * queued behind it on the store, so a quick rejoin reads what was just written.
     */
    public void saveAndUncache(UUID uuid) {
        PlayerQuestData data = dataMap.remove(uuid);
        if (data != null) saveAsync(data);
    }

    public record RankEntry(UUID uuid, int position, long exp) {
    }

    private static final String RANKING_TABLE = "adventure_ranking";

    private RankEntry rankEntry(ResultSet row) throws SQLException {
        return new RankEntry(UUID.fromString(row.getString("uuid")), row.getInt("position"), row.getLong("exp"));
    }

    /**
     * Rebuilds the frozen ranking of every player with at least {@code minExp} quest exp, highest first; ties go to
     * whoever reached their total first.
     */
    public CompletableFuture<Boolean> rebuildRanking(long minExp) {
        return store.executeAsync(List.of(
                SqlStatement.of("DELETE FROM " + RANKING_TABLE),
                new SqlStatement("INSERT INTO " + RANKING_TABLE + " (uuid, position, exp) "
                        + "SELECT {key}, ROW_NUMBER() OVER (ORDER BY exp DESC, updated_at ASC), exp FROM ("
                        + "SELECT {key}, json_extract(data, '$.exp') AS exp, COALESCE(json_extract(data, '$.expUpdatedAt'), 0) AS updated_at "
                        + "FROM {table} WHERE json_extract(data, '$.exp') >= ?)",
                        statement -> statement.setLong(1, Math.max(1, minExp)))
        ));
    }

    public CompletableFuture<Boolean> isRankingEmpty() {
        return store.queryAsync("SELECT 1 FROM " + RANKING_TABLE + " LIMIT 1", statement -> {
        }, row -> true).thenApply(List::isEmpty);
    }

    public CompletableFuture<List<RankEntry>> topRanked(int limit) {
        return store.queryAsync("SELECT uuid, position, exp FROM " + RANKING_TABLE + " WHERE position <= ? ORDER BY position",
                statement -> statement.setInt(1, limit), this::rankEntry);
    }

    public CompletableFuture<@Nullable RankEntry> rankingOf(UUID uuid) {
        return store.queryAsync("SELECT uuid, position, exp FROM " + RANKING_TABLE + " WHERE uuid = ?",
                statement -> statement.setString(1, uuid.toString()), this::rankEntry).thenApply(rows -> rows.isEmpty() ? null : rows.getFirst());
    }

    public void close() {
        store.close();
        groupStore.close();
    }
}
