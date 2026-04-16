package com.roguesmp.registry.ability;

import com.google.gson.JsonObject;
import com.roguesmp.player.ability.upgrade.ExpRequirement;
import com.roguesmp.player.ability.upgrade.ItemRequirement;
import com.roguesmp.player.ability.upgrade.UpgradeRequirement;
import com.roguesmp.registry.ItemRegistry;

import java.util.HashMap;
import java.util.Map;

public class UpgradeRequirementRegistry {

    @FunctionalInterface
    public interface RequirementDeserializer {
        UpgradeRequirement deserialize(JsonObject json);
    }

    private static final Map<String, RequirementDeserializer> registry = new HashMap<>();

    static {
        // Register existing types
        register("item", json -> {
            String itemId = json.get("item_id").getAsString();
            int amount = json.get("amount").getAsInt();
            return new ItemRequirement(ItemRegistry.getInstance().getBaseItem(itemId), amount);
        });

        register("exp", json -> {
            int level = json.get("level").getAsInt();
            return new ExpRequirement(level);
        });
    }

    public static void register(String type, RequirementDeserializer deserializer) {
        registry.put(type.toUpperCase(), deserializer);
    }

    public static UpgradeRequirement create(String type, JsonObject json) {
        RequirementDeserializer deserializer = registry.get(type.toUpperCase());
        if (deserializer == null) {
            throw new IllegalArgumentException("Unknown requirement type: " + type);
        }
        return deserializer.deserialize(json);
    }
}
