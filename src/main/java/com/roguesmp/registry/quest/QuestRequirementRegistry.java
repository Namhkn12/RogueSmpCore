package com.roguesmp.registry.quest;

import com.google.gson.JsonObject;
import com.roguesmp.quest.QuestRequirement;
import com.roguesmp.quest.requirement.DateRequirement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class QuestRequirementRegistry {

    @FunctionalInterface
    public interface Deserializer {
        @NotNull QuestRequirement deserialize(JsonObject jsonObject);
    }

    private static Map<String, Deserializer> registry = new HashMap<>();

    static {
        register("date_single", jsonObject -> {
            long timestamp = jsonObject.get("timestamp") != null ? jsonObject.get("timestamp").getAsLong() : -1;
            return new DateRequirement(timestamp);
        });
    }

    public static @Nullable QuestRequirement create(String typeName, JsonObject jsonObject) {
        Deserializer deserializer = registry.get(typeName);
        if (deserializer == null) {
            return null;
        }
        return deserializer.deserialize(jsonObject);
    }

    private static void register(String typeName, Deserializer deserializer) {
        registry.put(typeName, deserializer);
    }
}
