package com.roguesmp.quest;

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

public class QuestDataManager {

    private final Map<UUID, PlayerQuestData> dataMap = new HashMap<>();
    private final JsonSqliteStore<PlayerQuestData> store;

    public QuestDataManager(RogueSmpCore plugin) {
        this.store = new JsonSqliteStore<>(plugin, "player_quest_data.db", "player_quest_data", "uuid", PlayerQuestData.CODEC);
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
        store.close();
    }
}
