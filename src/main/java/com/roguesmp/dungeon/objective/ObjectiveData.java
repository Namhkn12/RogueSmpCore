package com.roguesmp.dungeon.objective;

import java.util.Map;

public class ObjectiveData {
    private String type;           // "SPAWNER_BREAK", "KILL_ALL", "FIND_ITEM"...
    private Map<String, Object> params; // dữ liệu tuỳ theo type

    // ví dụ SpawnerBreak: params = { "count": 3 }
    // ví dụ KillAll:       params = { "mobType": "ZOMBIE", "amount": 5 }


    public ObjectiveData() {
    }

    public ObjectiveData(String type, Map<String, Object> params) {
        this.type = type;
        this.params = params;
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
        this.params = params;
    }
}
