package com.roguesmp.dungeon.objective_.param;

import java.util.List;
import java.util.Map;

public class ObjectiveParams {
    private final Map<String, Object> raw;

    public ObjectiveParams(Map<String, Object> raw) {
        this.raw = raw != null ? raw : Map.of();
    }

    public int getInt(String key, int def) {
        Object v = raw.get(key);
        return v instanceof Number n ? n.intValue() : def;
    }

    public List<String> getStringList(String key) {
        Object v = raw.get(key);
        if (!(v instanceof List<?> list)) return List.of();
        return list.stream().map(Object::toString).toList();
    }
}
