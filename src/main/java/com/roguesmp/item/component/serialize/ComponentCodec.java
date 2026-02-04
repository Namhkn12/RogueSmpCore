package com.roguesmp.item.component.serialize;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.roguesmp.item.component.ItemComponent;

import java.lang.reflect.Type;
import java.util.function.Function;

public interface ComponentCodec<T extends ItemComponent> {

    T deserialize(JsonElement json, JsonDeserializationContext ctx);

    JsonElement serialize(T component, JsonSerializationContext ctx);

    static <T extends ItemComponent, V> ComponentCodec<T> singleArg(
            Type valueType,
            Function<V, T> constructor,
            Function<T, V> extractor
    ) {
        return new ComponentCodec<>() {

            @Override
            public T deserialize(JsonElement json, JsonDeserializationContext ctx) {
                V value = ctx.deserialize(json, valueType);
                return constructor.apply(value);
            }

            @Override
            public JsonElement serialize(T component, JsonSerializationContext ctx) {
                return ctx.serialize(extractor.apply(component), valueType);
            }
        };
    }
}

