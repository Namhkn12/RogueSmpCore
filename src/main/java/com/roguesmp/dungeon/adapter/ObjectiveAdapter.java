package com.roguesmp.dungeon.adapter;

import com.google.gson.*;
import com.roguesmp.dungeon.objective.IObjective;
import com.roguesmp.dungeon.objective.obj.SpawnerBreakObj;

import java.lang.reflect.Type;

public class ObjectiveAdapter implements JsonDeserializer<IObjective>, JsonSerializer<IObjective> {

    @Override
    public IObjective deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        String type = obj.get("type").getAsString();

        return switch (type) {
            case "spawner_break" -> context.deserialize(obj, SpawnerBreakObj.class);
            // thêm case cho các objective khác sau này
            default -> throw new JsonParseException("Unknown objective type: " + type);
        };
    }

    @Override
    public JsonElement serialize(IObjective src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj;

        if (src instanceof SpawnerBreakObj) {
            obj = context.serialize(src, SpawnerBreakObj.class).getAsJsonObject();
            obj.addProperty("type", "spawner_break");
        } else {
            throw new JsonParseException("Unknown IObjective implementation: " + src.getClass());
        }

        return obj;
    }
}
