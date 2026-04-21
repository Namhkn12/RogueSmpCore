package com.roguesmp.dungeon.data.definition.room.roomevent.factory;

import java.util.LinkedHashMap;
import java.util.Map;

public class RoomEventConfig {

    private String type;
    private Map<String, Object> params;

    public RoomEventConfig() {
        this.params = new LinkedHashMap<>();
    }

    public RoomEventConfig(String type, Map<String, Object> params) {
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
}
