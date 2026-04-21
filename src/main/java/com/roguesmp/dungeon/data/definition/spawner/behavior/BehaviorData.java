package com.roguesmp.dungeon.data.definition.spawner.behavior;

import java.util.Collections;
import java.util.Map;

public class BehaviorData {

    private String type;
    private Map<String, Object> params;

    public BehaviorData() {}

    public BehaviorData(String type, Map<String, Object> params) {
        this.type   = type;
        this.params = params;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Map<String, Object> getParams() { return params != null ? params : Collections.emptyMap(); }
    public void setParams(Map<String, Object> params) { this.params = params; }

    // ── Type-safe helpers ────────────────────────────────────────────────────

    public int getInt(String key, int def) {
        Object v = getParams().get(key);
        return v instanceof Number n ? n.intValue() : def;
    }

    public double getDouble(String key, double def) {
        Object v = getParams().get(key);
        return v instanceof Number n ? n.doubleValue() : def;
    }

    public String getString(String key, String def) {
        Object v = getParams().get(key);
        return v != null ? v.toString() : def;
    }

    public boolean getBoolean(String key, boolean def) {
        Object v = getParams().get(key);
        return v instanceof Boolean b ? b : def;
    }
}