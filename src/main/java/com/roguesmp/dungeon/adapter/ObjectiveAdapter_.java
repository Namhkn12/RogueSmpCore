package com.roguesmp.dungeon.adapter;

import com.google.gson.*;
import com.roguesmp.dungeon.objective_.IObjective;
import com.roguesmp.dungeon.objective_.ObjectiveFactory;
import com.roguesmp.dungeon.objective_.param.ObjectiveData;

import java.lang.reflect.Type;

public class ObjectiveAdapter_ implements JsonDeserializer<IObjective>, JsonSerializer<IObjective> {

    @Override
    public IObjective deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        JsonObject obj = json.getAsJsonObject();
        ObjectiveData data = context.deserialize(obj, ObjectiveData.class);
        return ObjectiveFactory.create(data);
    }

    @Override
    public JsonElement serialize(IObjective src, Type typeOfSrc, JsonSerializationContext context) {
        return context.serialize(src.getData(), ObjectiveData.class);
    }
}