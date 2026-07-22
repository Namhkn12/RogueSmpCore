package com.roguesmp.registry.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.roguesmp.quest.QuestReward;
import com.roguesmp.quest.reward.ItemReward;
import com.roguesmp.quest.reward.MoneyReward;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class QuestRewardRegistry {

    @FunctionalInterface
    public interface Deserializer {
        @NotNull QuestReward deserialize(JsonObject jsonObject);
    }

    private static Map<String, Deserializer> registry = new HashMap<>();

    static {
        register("item", jsonObject -> {
            JsonObject itemMapJson = jsonObject.getAsJsonObject("item");
            Map<String, Integer> items = new LinkedHashMap<>();
            for (String itemId : itemMapJson.keySet()) {
                int amount = itemMapJson.get(itemId).getAsInt();
                items.put(itemId, amount);
            }
            return new ItemReward(items);
        });

        register("money", jsonObject -> {
            JsonElement amount = jsonObject.getAsJsonPrimitive("amount");
            return new MoneyReward(amount.getAsLong());
        });

    }

    public static @Nullable QuestReward create(String typeName, JsonObject jsonObject) {
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
