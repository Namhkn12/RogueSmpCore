package com.roguesmp.player;

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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerDataManager {

    private final Map<UUID, PlayerData> playerDataCache = new HashMap<>();
    private final JsonSqliteStore<PlayerData> store = new JsonSqliteStore<>(RogueSmpCore.getInstance(), "player_data.db", "player_data", "uuid", PlayerData.CODEC);

    public void cacheData(PlayerData data) {
        playerDataCache.put(data.getUuid(), data);
    }

    public PlayerData removeCachedData(UUID uuid) {
        return playerDataCache.remove(uuid);
    }

    /**
     * Main thread only: the data is encoded immediately, then queued on the store in call order.
     */
    public void saveAsync(PlayerData playerData) {
        String json = store.encode(playerData);
        if (json == null) return;

        playerData.setDirty(false);
        store.writeAsync(playerData.getUuid(), json).thenAccept(written -> {
            if (!written) playerData.setDirty(true);
        });
    }

    public @Blocking void savePlayerData(PlayerData playerData) {
        if (playerData == null) return;

        String json = store.encode(playerData);
        if (json == null) return;

        if (store.write(playerData.getUuid(), json)) playerData.setDirty(false);
    }

    /**
     * Main thread only: the data is encoded immediately and all players are written in one transaction.
     */
    public CompletableFuture<Void> saveAllAsync(Collection<PlayerData> players) {
        Map<UUID, String> rows = new HashMap<>();
        List<PlayerData> encoded = new ArrayList<>();
        for (PlayerData playerData : players) {
            String json = store.encode(playerData);
            if (json == null) continue;

            playerData.setDirty(false);
            rows.put(playerData.getUuid(), json);
            encoded.add(playerData);
        }

        return store.writeAllAsync(rows).thenAccept(written -> {
            if (!written) encoded.forEach(playerData -> playerData.setDirty(true));
        });
    }

    public @Blocking PlayerData loadPlayerData(UUID uuid) {
        PlayerData loaded = store.load(uuid);
        return loaded != null ? loaded : new PlayerData(uuid);
    }

    public @Nullable PlayerData getData(UUID uuid) {
        return playerDataCache.get(uuid);
    }

    public @Unmodifiable Collection<PlayerData> getCachedPlayerData() {
        return Collections.unmodifiableCollection(playerDataCache.values());
    }

    public void close() {
        store.close();
    }
}
