package com.roguesmp.registry.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.quest.*;
import com.roguesmp.utils.Utils;
import org.bukkit.Material;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.*;

public class QuestRegistry {

    private static QuestRegistry INSTANCE;

    public static String FOLDER_NAME = "quests";

    private final RogueSmpCore plugin;

    private final Map<String, Quest> registry = new HashMap<>();

    private QuestRegistry(RogueSmpCore plugin) {
        this.plugin = plugin;
    }

    public void register(String questId, Quest quest) {
        registry.put(questId, quest);
    }

    public @Blocking void loadQuest() {
        File folder = new File(plugin.getDataFolder(), FOLDER_NAME);

        RogueSmpCore.LOGGER.info("Loading quest registry...");
        if (!folder.exists()) {
            folder.mkdirs();
            RogueSmpCore.LOGGER.info("Folder {} not found, creating new...", FOLDER_NAME);
            return;
        }

        registry.clear();

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;
        int count = 0;
        for (File file : files) {
            try (Reader reader = new FileReader(file)) {
                JsonObject jsonObject = Utils.GSON.fromJson(reader, JsonObject.class);

                if (!jsonObject.has("id")) {
                    RogueSmpCore.LOGGER.warn("Skipping file {}: Missing required 'id' fields.", file.getName());
                    continue;
                }

                String id = jsonObject.get("id").getAsString();
                String name = jsonObject.has("name") ? jsonObject.get("name").getAsString() : null;
                String materialStr = jsonObject.has("icon") ? jsonObject.get("icon").getAsString() : null;
                if (materialStr == null) materialStr = "PAPER";
                Material materialIcon = Material.valueOf(materialStr);

                List<String> description = new ArrayList<>();
                if (jsonObject.has("description") && jsonObject.get("description").isJsonArray()) {
                    JsonArray descArray = jsonObject.getAsJsonArray("description");
                    for (JsonElement element : descArray) {
                        description.add(element.getAsString());
                    }
                }

                // Process Objectives JSON Map Layout
                Map<String, QuestObjective> objectives = new LinkedHashMap<>();
                if (jsonObject.has("objectives") && jsonObject.get("objectives").isJsonObject()) {
                    JsonObject objectivesMapObject = jsonObject.getAsJsonObject("objectives");
                    Set<String> objectiveKeys = objectivesMapObject.keySet();

                    for (String trackingKey : objectiveKeys) {
                        JsonObject objectiveNode = objectivesMapObject.getAsJsonObject(trackingKey);

                        if (!objectiveNode.has("type")) {
                            RogueSmpCore.LOGGER.warn("Objective '{}' in quest '{}' is missing its 'type' parameter. Skipping it.", trackingKey, id);
                            continue;
                        }
                        String type = objectiveNode.get("type").getAsString();

                        QuestObjective objective = QuestObjectiveRegistry.create(type, objectiveNode);
                        if (objective == null) {
                            RogueSmpCore.LOGGER.warn("Objective '{}' in quest '{}' has unknown 'type' ({}). Skipping it.", trackingKey, id, type);
                            continue;
                        }

                        objectives.put(trackingKey, objective);
                    }
                } else {
                    RogueSmpCore.LOGGER.warn("Quest '{}' loaded with zero active objectives configured.", id);
                }

                //Process Reward array
                List<QuestReward> rewards = new ArrayList<>();
                if (jsonObject.has("rewards") && jsonObject.get("rewards").isJsonArray()) {
                    JsonArray rewardsArray = jsonObject.getAsJsonArray("rewards");
                    for (JsonElement rewardElement : rewardsArray) {
                        JsonObject rewardNode = rewardElement.getAsJsonObject();

                        if (!rewardNode.has("type")) {
                            RogueSmpCore.LOGGER.warn("A reward in quest '{}' is missing its 'type' parameter. Skipping it.", id);
                            continue;
                        }
                        String type = rewardNode.get("type").getAsString();

                        QuestReward reward = QuestRewardRegistry.create(type, rewardNode);
                        if (reward == null) {
                            RogueSmpCore.LOGGER.warn("A reward in quest '{}' has unknown 'type' ({}). Skipping it.", id, type);
                            continue;
                        }
                        rewards.add(reward);
                    }
                }

                //Process Requirement array
                List<QuestRequirement> requirements = new ArrayList<>();
                if (jsonObject.has("requirements") && jsonObject.get("requirements").isJsonArray()) {
                    JsonArray requirementArray = jsonObject.getAsJsonArray("requirements");
                    for (JsonElement requirementElement : requirementArray) {
                        JsonObject requirementNode = requirementElement.getAsJsonObject();

                        if (!requirementNode.has("type")) {
                            RogueSmpCore.LOGGER.warn("A requirement in quest '{}' is missing its 'type' parameter. Skipping it.", id);
                            continue;
                        }
                        String type = requirementNode.get("type").getAsString();

                        QuestRequirement requirement = QuestRequirementRegistry.create(type, requirementNode);
                        if (requirement == null) {
                            RogueSmpCore.LOGGER.warn("A requirement in quest '{}' has unknown 'type' ({}). Skipping it.", id, type);
                            continue;
                        }
                        requirements.add(requirement);
                    }
                }

                Quest quest = new Quest(id, name, materialIcon, description, requirements, objectives, rewards);
                registry.put(id, quest);
                count++;

            } catch (Exception e) {
                RogueSmpCore.LOGGER.warn("Could not load quest from {}: {}", file.getName(), e.getMessage());
                e.printStackTrace();
            }
        }

        RogueSmpCore.LOGGER.info("Loaded {} quests.", count);
    }

    public @Nullable Quest getQuest(String questId) {
        return registry.get(questId);
    }

    public @Unmodifiable Map<String, Quest> getRegistry() {
        return Collections.unmodifiableMap(registry);
    }

    public static void init(RogueSmpCore plugin) {
        INSTANCE = new QuestRegistry(plugin);
    }

    public static QuestRegistry getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("QuestRegistry is null!");
        }
        return INSTANCE;
    }
}
