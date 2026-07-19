package com.roguesmp.registry.npc;

import com.google.gson.JsonObject;
import com.roguesmp.npc.action.NpcAction;
import com.roguesmp.npc.action.OpenGuiInteractAction;
import com.roguesmp.npc.action.RunCommandInteractAction;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Hold action type definition
 */
public class NpcActionRegistry {

    @FunctionalInterface
    public interface NpcActionDeserializer {
        NpcAction deserialize(JsonObject jsonObject);
    }

    private static final Map<String, NpcActionDeserializer> map = new HashMap<>();

    static {
        register("run_command", jsonObject -> {
            String command = jsonObject.get("command") == null ? "" : jsonObject.get("command").getAsString();
            return new RunCommandInteractAction(command);
        });

        register("open_gui", jsonObject -> {
            String id = jsonObject.get("gui") == null ? "" : jsonObject.get("gui").getAsString();
            return new OpenGuiInteractAction(id);
        });
    }

    private static void register(String key, NpcActionDeserializer deserializer) {
        map.put(key, deserializer);
    }

    public static @Nullable NpcAction create(String key, JsonObject object) {
        NpcActionDeserializer deserializer = map.get(key);
        if (deserializer == null) {
            throw new IllegalArgumentException("Unknown npc action type: " + key);
        }
        return deserializer.deserialize(object);
    }
}
