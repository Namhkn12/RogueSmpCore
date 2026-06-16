package com.roguesmp.registry.quest;

import com.google.gson.JsonObject;
import com.roguesmp.quest.ObjectiveProgress;
import com.roguesmp.quest.QuestObjective;
import com.roguesmp.quest.objective.KillMobObjective;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class QuestObjectiveRegistry {

    @FunctionalInterface
    public interface Deserializer {
        @NotNull QuestObjective deserialize(JsonObject jsonObject);
    }

    private static final Map<String, Deserializer> registry = new HashMap<>();

    static {
        register("kill_mob", jsonObject -> {
            String mobId = jsonObject.get("mobId") == null ? null : jsonObject.get("mobId").getAsString();
            int amount = jsonObject.get("amount") == null ? -1 : jsonObject.get("amount").getAsInt();
            if (mobId == null) {
                return new KillMobObjective("dummy", amount);
            } else return new KillMobObjective(mobId, amount);

        });
    }

    public static @Nullable QuestObjective create(String typeName, JsonObject jsonObject) {
        Deserializer deserializer = registry.get(typeName.toLowerCase());
        if (deserializer == null) {
            return null;
        }
        return deserializer.deserialize(jsonObject);
    }

    private static void register(String typeName, Deserializer deserializer) {
        registry.put(typeName, deserializer);
    }
}
