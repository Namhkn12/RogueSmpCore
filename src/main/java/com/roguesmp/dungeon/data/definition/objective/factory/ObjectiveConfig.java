package com.roguesmp.dungeon.data.definition.objective.factory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Raw serialized configuration for building an objective instance.
 */
public class ObjectiveConfig {

    private String type;
    private Map<String, Object> params;

    public ObjectiveConfig() {
        this.params = new LinkedHashMap<>();
    }

    public ObjectiveConfig(String type, Map<String, Object> params) {
        this.type = type;
        this.params = params != null ? new LinkedHashMap<>(params) : new LinkedHashMap<>();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params != null ? new LinkedHashMap<>(params) : new LinkedHashMap<>();
    }

    public Object getParam(String key, Object defaultValue) {
        return params.getOrDefault(key, defaultValue);
    }

    public int getIntParam(String key, int defaultValue) {
        Object value = params.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }

    public double getDoubleParam(String key, double defaultValue) {
        Object value = params.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return defaultValue;
    }

    public String getStringParam(String key, String defaultValue) {
        Object value = params.get(key);
        return value instanceof String string ? string : defaultValue;
    }

    public Map<String, Integer> getIntMapParam(String key) {
        Object value = params.get(key);
        Map<String, Integer> result = new LinkedHashMap<>();
        if (!(value instanceof Map<?, ?> rawMap)) {
            return result;
        }

        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() instanceof String stringKey && entry.getValue() instanceof Number numberValue) {
                result.put(stringKey, numberValue.intValue());
            }
        }
        return result;
    }

    public List<String> getStringListParam(String key) {
        Object value = params.get(key);
        List<String> result = new ArrayList<>();
        if (!(value instanceof List<?> rawList)) {
            return result;
        }

        for (Object item : rawList) {
            if (item instanceof String stringValue) {
                result.add(stringValue);
            }
        }
        return result;
    }
}
