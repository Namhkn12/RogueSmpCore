package com.roguesmp.registry;

import com.roguesmp.item.component.ComponentKey;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.component.serialize.ComponentCodec;
import com.roguesmp.item.component.serialize.DefaultGsonCodec;

import java.util.HashMap;
import java.util.Map;

public class ItemComponentCodecRegistry {

    private static final Map<String, ComponentCodec<?>> CODECS = new HashMap<>();

    public static <T extends ItemComponent> ComponentKey<T> register(String id, ComponentCodec<T> codec) {
        if (CODECS.containsKey(id)) {
            throw new IllegalStateException("Duplicate component key: " + id);
        }

        ComponentKey<T> key = new ComponentKey<>(id);
        CODECS.put(id, codec);
        return key;
    }

    public static <T extends ItemComponent> ComponentKey<T> register(String id, Class<T> tClass) {
        if (CODECS.containsKey(id)) {
            throw new IllegalStateException("Duplicate component key: " + id);
        }

        ComponentKey<T> key = new ComponentKey<>(id);
        CODECS.put(id, new DefaultGsonCodec<>(tClass));
        return key;
    }

    @SuppressWarnings("unchecked")
    public static <T extends ItemComponent> ComponentCodec<T> getCodec(String key) {
        return (ComponentCodec<T>) CODECS.get(key);
    }
}
