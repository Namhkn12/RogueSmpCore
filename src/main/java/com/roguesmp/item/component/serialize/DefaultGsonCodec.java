package com.roguesmp.item.component.serialize;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.roguesmp.item.component.ItemComponent;

public final class DefaultGsonCodec<T extends ItemComponent> implements ComponentCodec<T> {

    private final Class<T> type;

    public DefaultGsonCodec(Class<T> type) {
        this.type = type;
    }

    @Override
    public T deserialize(JsonElement json, JsonDeserializationContext ctx) {
        return ctx.deserialize(json, type);
    }

    @Override
    public JsonElement serialize(T component, JsonSerializationContext ctx) {
        return ctx.serialize(component);
    }
}

