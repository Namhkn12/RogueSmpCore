package com.roguesmp.island;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.storage.JsonSqliteStore;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class IslandDataManager {

    private final Map<UUID, IslandData> islandDataCache = new ConcurrentHashMap<>();
    private final JsonSqliteStore<IslandData> store;

    public IslandDataManager(RogueSmpCore plugin) {
        this.store = new JsonSqliteStore<>(plugin, "island_data.db", "island_data", "island_id", IslandData.CODEC);
    }

    /**
     * Must be called on the main thread: the data is encoded immediately so the async write never sees a half-mutated object.
     */
    public void saveAsync(IslandData islandData) {
        String json = store.encode(islandData);
        if (json == null) return;

        islandData.setDirty(false);
        store.writeAsync(islandData.getIslandId(), json).thenAccept(written -> {
            if (!written) islandData.setDirty(true);
        });
    }

    /**
     * Main thread only, like {@link #saveAsync}. All islands are written in one transaction.
     */
    public CompletableFuture<Void> saveAllAsync(Collection<IslandData> islands) {
        Map<UUID, String> rows = new HashMap<>();
        List<IslandData> encoded = new ArrayList<>();
        for (IslandData islandData : islands) {
            String json = store.encode(islandData);
            if (json == null) continue;

            islandData.setDirty(false);
            rows.put(islandData.getIslandId(), json);
            encoded.add(islandData);
        }

        return store.writeAllAsync(rows).thenAccept(written -> {
            if (!written) encoded.forEach(islandData -> islandData.setDirty(true));
        });
    }

    public @Blocking void saveNow(IslandData islandData) {
        String json = store.encode(islandData);
        if (json == null) return;

        islandData.setDirty(false);
        if (!store.write(islandData.getIslandId(), json)) islandData.setDirty(true);
    }

    public @Nullable @Blocking IslandData loadIslandData(@Nullable UUID islandId) {
        if (islandId == null) return null;

        IslandData cached = islandDataCache.get(islandId);
        return cached != null ? cached : store.load(islandId);
    }

    public @Blocking Set<UUID> findArchivedIslandIds() {
        Set<UUID> archived = new HashSet<>();
        for (IslandData data : islandDataCache.values()) {
            if (data.isArchived()) archived.add(data.getIslandId());
        }
        store.loadAll().forEach((islandId, data) -> {
            if (data.isArchived()) archived.add(islandId);
        });
        return archived;
    }

    public @Blocking void delete(UUID islandId) {
        islandDataCache.remove(islandId);
        store.delete(islandId);
    }

    public @Nullable IslandData getCachedData(@Nullable UUID islandId) {
        return islandId == null ? null : islandDataCache.get(islandId);
    }

    public @Unmodifiable Map<UUID, IslandData> getIslandDataCache() {
        return Collections.unmodifiableMap(islandDataCache);
    }

    public @Nullable IslandData removeCache(UUID uuid) {
        return islandDataCache.remove(uuid);
    }

    /**
     * @return the cached instance for this island, which is the given one unless another thread cached it first
     */
    public IslandData cache(IslandData islandData) {
        IslandData existing = islandDataCache.putIfAbsent(islandData.getIslandId(), islandData);
        return existing != null ? existing : islandData;
    }

    public void onDisable() {
        for (IslandData islandData : islandDataCache.values()) {
            if (islandData.isDirty()) saveNow(islandData);
        }
        store.close();
    }
}
