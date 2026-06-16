package com.roguesmp.quest;

import com.google.gson.JsonObject;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.registry.quest.QuestRegistry;
import com.roguesmp.utils.Utils;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class QuestDataManager {

    public static final String FOLDER_NAME = "player_quest_data";

    private final Map<UUID, PlayerQuestData> dataMap = new HashMap<>();

    private final RogueSmpCore plugin;
    private final QuestRegistry questRegistry;

    public QuestDataManager(RogueSmpCore plugin, QuestRegistry questRegistry) {
        this.plugin = plugin;
        this.questRegistry = questRegistry;
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
            JsonObject data = Utils.GSON.fromJson(reader, JsonObject.class);

            if (data == null) {
                RogueSmpCore.LOGGER.info("Player quest data is empty, creating default data");
                return createDefault(uuid);
            }

            Set<String> questIds = data.keySet();
            Map<String, QuestProgress> progressMap = new HashMap<>();
            for (String questId : questIds) {
                Quest quest = questRegistry.getQuest(questId);
                if (quest == null) {
                    RogueSmpCore.LOGGER.warn("Player has unknown quest id: {}", questId);
                    continue;
                }

                JsonObject questProgressJson = data.getAsJsonObject(questId);

                boolean completed = questProgressJson.has("completed") && questProgressJson.get("completed").getAsBoolean();
                boolean rewardClaimed = questProgressJson.has("rewardClaimed") && questProgressJson.get("rewardClaimed").getAsBoolean();

                JsonObject progressJson = questProgressJson.getAsJsonObject("progress");
                if (progressJson == null) {
                    RogueSmpCore.LOGGER.warn("Found no progress data for quest '{}'. Skipping it.", questId);
                    continue;
                }

                Map<String, QuestObjective> questObjectiveMap = quest.getObjectives();
                Map<String, ObjectiveProgress> objectiveProgressMap = new HashMap<>();
                Set<String> progressIds = progressJson.keySet();
                for (String progressId : progressIds) {
                    JsonObject jsonObject = progressJson.getAsJsonObject(progressId);
                    QuestObjective objective = questObjectiveMap.get(progressId);
                    if (objective == null) {
                        RogueSmpCore.LOGGER.warn("Found no matching objective id for progress data id '{}' for quest '{}'. Skipping it.", progressId, questId);
                        continue;
                    }
                    ObjectiveProgress objectiveProgress = objective.deserializeProgress(jsonObject);

                    objectiveProgressMap.put(progressId, objectiveProgress);
                }


                progressMap.put(questId, new QuestProgress(quest, completed, rewardClaimed, objectiveProgressMap));
            }

            PlayerQuestData playerQuestData = new PlayerQuestData(uuid, progressMap);

            RogueSmpCore.LOGGER.info("Loaded player quest data (uuid: {})", uuid);
            return playerQuestData;

        } catch (IOException e) {
            e.printStackTrace();
            return createDefault(uuid);
        }
    }

    public @Blocking void saveData(PlayerQuestData playerQuestData) {
        PlayerQuestData clone = new PlayerQuestData(playerQuestData.getUuid(), playerQuestData.getQuestProgresses());
        File folder = new File(plugin.getDataFolder(), FOLDER_NAME);
        File file = new File(folder, clone.getUuid() + ".json");
        RogueSmpCore.LOGGER.info("Saving player quest data (uuid: {})", clone.getUuid());
        try (Writer writer = new FileWriter(file)) {

            JsonObject result = new JsonObject();
            Map<String, QuestProgress> progressMap = clone.getQuestProgresses();

            for (Map.Entry<String, QuestProgress> entry : progressMap.entrySet()) {

                String questId = entry.getKey();
                QuestProgress questProgress = entry.getValue();

                result.add(questId, questProgress.serialize());
            }

            Utils.GSON.toJson(result, writer);

        } catch (IOException e) {
            RogueSmpCore.LOGGER.warn("Failed to save player quest data (uuid: {})", clone.getUuid());
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
