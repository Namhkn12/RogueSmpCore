package com.roguesmp.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.JsonOps;
import com.roguesmp.utils.Utils;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QuestDataManager {

    public static final String FOLDER_NAME = "player_quest_data";

    private final Map<UUID, PlayerQuestData> dataMap = new HashMap<>();

    private final RogueSmpCore plugin;

    public QuestDataManager(RogueSmpCore plugin) {
        this.plugin = plugin;
    }

    public @Blocking PlayerQuestData loadData(UUID uuid) {
        File folder = new File(plugin.getDataFolder(), FOLDER_NAME);

        RogueSmpCore.LOGGER.info("Loading quest data for uuid {}", uuid);
        if (!folder.exists()) {
            folder.mkdirs();
            RogueSmpCore.LOGGER.info("Folder {} not found, creating new...", FOLDER_NAME);
            return createDefault(uuid);
        }

        File file = new File(folder, uuid + ".json");

        if (!file.exists()) {
            return createDefault(uuid);
        }

        try (Reader reader = new FileReader(file)) {
            JsonObject json = Utils.GSON.fromJson(reader, JsonObject.class);

            if (json == null) return createDefault(uuid);
            DataResult<PlayerQuestData> result = PlayerQuestData.CODEC.decode(json, JsonOps.INSTANCE);
            if (!result.isSuccess()) {
                RogueSmpCore.LOGGER.warn("Failed to decode quest data for {}: {}", uuid, result.error());
                return createDefault(uuid);
            }
            return result.result();

        } catch (IOException e) {
            e.printStackTrace();
            return createDefault(uuid);
        }
    }

    public @Blocking void saveData(PlayerQuestData playerQuestData) {
        File folder = new File(plugin.getDataFolder(), FOLDER_NAME);
        File file = new File(folder, playerQuestData.getUuid() + ".json");
        PlayerQuestData data = new PlayerQuestData(playerQuestData);
        RogueSmpCore.LOGGER.info("Saving player quest data (uuid: {})", playerQuestData.getUuid());

        try (Writer writer = new FileWriter(file)) {

            DataResult<JsonElement> encoded = PlayerQuestData.CODEC.encode(data, JsonOps.INSTANCE);
            if (encoded.isSuccess()) {
                Utils.GSON.toJson(encoded.result(), writer);
            } else {
                RogueSmpCore.LOGGER.error(encoded.error());
            }

        } catch (IOException e) {
            RogueSmpCore.LOGGER.warn("Failed to save player quest data (uuid: {})", playerQuestData.getUuid());
            e.printStackTrace();
        }
    }

    public @Blocking void saveAllData() {
        dataMap.forEach((uuid, playerQuestData) -> {
            saveData(playerQuestData);
        });
    }

    public @Nullable PlayerQuestData getCachedData(UUID uuid) {
        return dataMap.get(uuid);
    }

    public void cache(PlayerQuestData playerQuestData) {
        dataMap.put(playerQuestData.getUuid(), playerQuestData);
    }

    private PlayerQuestData createDefault(UUID uuid) {
        return new PlayerQuestData(uuid);
    }
}
