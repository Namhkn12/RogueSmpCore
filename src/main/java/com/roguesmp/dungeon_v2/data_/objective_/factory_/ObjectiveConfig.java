package com.roguesmp.dungeon_v2.data_.objective_.factory_;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectiveConfig {

    private String type;
    private Map<String, Object> params;

    public ObjectiveConfig() {}

    public ObjectiveConfig(String type, Map<String, Object> params) {
        this.type = type;
        this.params = params != null ? params : Map.of();
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }

    public Object getParam(String key, Object defaultValue) {
        return params.getOrDefault(key, defaultValue);
    }

    public int getIntParam(String key, int defaultValue) {
        Object val = params.get(key);
        if (val == null) return defaultValue;
        return ((Number) val).intValue();
    }

    public double getDoubleParam(String key, double defaultValue) {
        Object val = params.get(key);
        if (val == null) return defaultValue;
        return ((Number) val).doubleValue();
    }

    public String getStringParam(String key, String defaultValue) {
        return (String) params.getOrDefault(key, defaultValue);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Integer> getIntMapParam(String key) {
        Object val = params.get(key);
        if (val == null) return new HashMap<>();
        return (Map<String, Integer>) val;
    }

    @SuppressWarnings("unchecked")
    public List<String> getStringListParam(String key) {
        Object val = params.get(key);
        if (val == null) return List.of();
        return (List<String>) val;
    }
}