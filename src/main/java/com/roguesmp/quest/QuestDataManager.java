package com.roguesmp.quest;

import com.google.gson.JsonObject;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.registry.quest.QuestRegistry;
import com.roguesmp.tag.SmpTag;
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
    private final QuestManager questManager;

    public QuestDataManager(RogueSmpCore plugin, QuestRegistry questRegistry, QuestManager questManager) {
        this.plugin = plugin;
        this.questRegistry = questRegistry;
        this.questManager = questManager;
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
            JsonObject rootJson = Utils.GSON.fromJson(reader, JsonObject.class);

            if (rootJson == null) {
                RogueSmpCore.LOGGER.info("Player quest data is empty, creating default data");
                return createDefault(uuid);
            }

            Map<String, QuestProgress> progressMap = new HashMap<>();

            if (rootJson.has("questProgress") && rootJson.get("questProgress").isJsonObject()) {
                JsonObject questsJson = rootJson.getAsJsonObject("questProgress");

                for (String questId : questsJson.keySet()) {
                    Quest quest = questRegistry.getQuest(questId);
                    if (quest == null) {
                        RogueSmpCore.LOGGER.warn("Player has unknown quest id: {}", questId);
                        continue;
                    }

                    JsonObject questProgressJson = questsJson.getAsJsonObject(questId);

                    boolean rewardClaimed = questProgressJson.has("rewardClaimed") && questProgressJson.get("rewardClaimed").getAsBoolean();
                    long acceptTimestamp = questProgressJson.has("acceptTimestamp") ? questProgressJson.get("acceptTimestamp").getAsLong() : -1;
                    long completedTimestamp = questProgressJson.has("completedTimestamp") ? questProgressJson.get("completedTimestamp").getAsLong() : -1;

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

                    progressMap.put(questId, new QuestProgress(quest, rewardClaimed, acceptTimestamp, completedTimestamp, objectiveProgressMap));
                }
            }

            // Deserialize daily completions map
            Map<String, Integer> dailyCompletionsMap = new HashMap<>();
            if (rootJson.has("dailyCompletions") && rootJson.get("dailyCompletions").isJsonObject()) {
                JsonObject dailyCompletionsJson = rootJson.getAsJsonObject("dailyCompletions");
                for (String tagId : dailyCompletionsJson.keySet()) {
                    dailyCompletionsMap.put(tagId, dailyCompletionsJson.get(tagId).getAsInt());
                }
            }

            long lastDailyCompletionResetTimestamp = rootJson.has("lastDailyCompletionResetTimestamp")
                    ? rootJson.get("lastDailyCompletionResetTimestamp").getAsLong()
                    : -1;

            PlayerQuestData playerQuestData = new PlayerQuestData(
                    uuid,
                    progressMap,
                    dailyCompletionsMap,
                    lastDailyCompletionResetTimestamp
            );

            RogueSmpCore.LOGGER.info("Loaded player quest data (uuid: {})", uuid);
            return playerQuestData;

        } catch (IOException e) {
            e.printStackTrace();
            return createDefault(uuid);
        }
    }

    public @Blocking void saveData(PlayerQuestData playerQuestData) {
        File folder = new File(plugin.getDataFolder(), FOLDER_NAME);
        File file = new File(folder, playerQuestData.getUuid() + ".json");
        RogueSmpCore.LOGGER.info("Saving player quest data (uuid: {})", playerQuestData.getUuid());

        try (Writer writer = new FileWriter(file)) {

            JsonObject rootJson = new JsonObject();
            JsonObject questsJson = new JsonObject();
            Map<String, QuestProgress> progressMap = playerQuestData.getQuestProgresses();

            for (Map.Entry<String, QuestProgress> entry : progressMap.entrySet()) {
                String questId = entry.getKey();
                QuestProgress questProgress = entry.getValue();

                questsJson.add(questId, questProgress.serialize());
            }

            rootJson.add("questProgress", questsJson);

            // Serialize daily completions map
            JsonObject dailyCompletionsJson = new JsonObject();
            for (SmpTag<Quest> tag : questManager.getDailyQuestManager().getDailyTags()) {
                int count = playerQuestData.getDailyCompletionsForTag(tag.getId());
                if (count > 0) {
                    dailyCompletionsJson.addProperty(tag.getId(), count);
                }
            }
            rootJson.add("dailyCompletions", dailyCompletionsJson);

            rootJson.addProperty("lastDailyCompletionResetTimestamp", playerQuestData.getLastDailyCompletionResetTimestamp());

            Utils.GSON.toJson(rootJson, writer);

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
