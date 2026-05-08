package com.roguesmp.registry.npc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.npc.BaseNpc;
import com.roguesmp.npc.action.NpcAction;
import com.roguesmp.utils.Utils;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NpcRegistry {

    private static NpcRegistry INSTANCE;
    private static final String FOLDER_NAME = "npcs";

    private final Map<String, BaseNpc> registry = new HashMap<>();

    private NpcRegistry() {

    }

    public static void init() {
        INSTANCE = new NpcRegistry();
    }

    public void loadData() {
        // 1. Setup the folder
        File folder = new File(RogueSmpCore.getInstance().getDataFolder(), FOLDER_NAME);
        RogueSmpCore.LOGGER.info("Loading npc data...");
        if (!folder.exists()) {
            folder.mkdirs();
            RogueSmpCore.LOGGER.info("Folder {} not found, creating new...", FOLDER_NAME);
            return;
        }

        // 2. Clear existing registry to support reloads
        registry.clear();

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;
        int count = 0;
        for (File file : files) {
            try (Reader reader = new FileReader(file)) {
                // 3. Parse the basic NPC structure
                JsonObject jsonObject = Utils.GSON.fromJson(reader, JsonObject.class);

                // Map JSON to BaseNpc fields
                String id = jsonObject.get("id").getAsString();
                String name = jsonObject.get("name").getAsString();
                EntityType type = EntityType.valueOf(jsonObject.get("entityType").getAsString().toUpperCase());
                String skinValue = jsonObject.has("skinValue") ? jsonObject.get("skinValue").getAsString() : "";
                String skinSignature = jsonObject.has("skinSignature") ? jsonObject.get("skinSignature").getAsString() : "";
                String description = jsonObject.has("description") ? jsonObject.get("description").getAsString() : "";

                // 4. Handle Actions List
                List<NpcAction> actions = new ArrayList<>();
                if (jsonObject.has("actions")) {
                    JsonArray actionsArray = jsonObject.getAsJsonArray("actions");
                    for (JsonElement element : actionsArray) {
                        JsonObject actionObj = element.getAsJsonObject();
                        String actionType = actionObj.get("type").getAsString();

                        // Use your NpcActionRegistry to create the specific implementation
                        NpcAction action = NpcActionRegistry.create(actionType, actionObj);
                        if (action != null) {
                            actions.add(action);
                        }
                    }
                }

                // 5. Construct and register
                BaseNpc npc = new BaseNpc(type, name, skinValue, skinSignature, description, id, actions);
                registry.put(id, npc);
                count++;
            } catch (Exception e) {
                RogueSmpCore.LOGGER.warn("Could not load NPC from {}: {}", file.getName(), e.getMessage());
                e.printStackTrace();
            }
        }
        RogueSmpCore.LOGGER.info("Loaded npc data ({} entries)", count);
    }

    public static NpcRegistry getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("NpcRegistry is null!");
        }
        return INSTANCE;
    }

    public @Nullable BaseNpc getBase(String id) {
        return registry.get(id);
    }
}
