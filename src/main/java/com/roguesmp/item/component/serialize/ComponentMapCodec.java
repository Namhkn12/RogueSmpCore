package com.roguesmp.item.component.serialize;

import com.google.gson.*;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.registry.ItemComponentCodecRegistry;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ComponentMapCodec implements JsonDeserializer<Map<String, ItemComponent>>, JsonSerializer<Map<String, ItemComponent>> {

    @Override
    public Map<String, ItemComponent> deserialize(
            JsonElement json,
            Type typeOfT,
            JsonDeserializationContext context
    ) {
        JsonObject obj = json.getAsJsonObject();
        Map<String, ItemComponent> result = new HashMap<>();

        for (var entry : obj.entrySet()) {
            String key = entry.getKey();
            ComponentCodec<?> codec = ItemComponentCodecRegistry.getCodec(key);

            if (codec == null) {
                throw new JsonParseException("Unknown component: " + key);
            }

            ItemComponent component = codec.deserialize(entry.getValue(), context);

            result.put(key, component);
        }

        return result;
    }

    @Override
    public JsonElement serialize(
            Map<String, ItemComponent> src,
            Type typeOfSrc,
            JsonSerializationContext context
    ) {
        JsonObject obj = new JsonObject();

        for (var entry : src.entrySet()) {
            String key = entry.getKey();
            ItemComponent component = entry.getValue();

            ComponentCodec<ItemComponent> codec = ItemComponentCodecRegistry.getCodec(key);

            if (codec == null) {
                throw new JsonParseException("Unknown component: " + key);
            }

            obj.add(key, codec.serialize(component, context));
        }

        return obj;
    }
}

