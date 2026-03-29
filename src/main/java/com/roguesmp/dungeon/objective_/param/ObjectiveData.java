package com.roguesmp.dungeon.objective_.param;

import java.util.Map;

public class ObjectiveData {
    private String type;
    private Map<String, Object> params;

    public ObjectiveData() {}

    public ObjectiveData(String type, Map<String, Object> params) {
        this.type = type;
        this.params = params;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public ObjectiveParams getParams() {
        return new ObjectiveParams(params);
    }

    public Map<String, Object> getRawParams() { return params; }

    public void setParams(Map<String, Object> params) { this.params = params; }
}