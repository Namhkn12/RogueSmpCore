package com.roguesmp.dungeon.behavior;

import java.util.Map;

public class BehaviorData {
    private String type;
    private Map<String, Object> params;

    public BehaviorData(){}

    public BehaviorData(String type, Map<String, Object> params){
        this.type = type;
        this.params = params;
    }

    public String getType() { return type; }
    public Map<String, Object> getParams() { return params; }
    public void setType(String type) { this.type = type; }
    public void setParams(Map<String, Object> params) { this.params = params; }
}
