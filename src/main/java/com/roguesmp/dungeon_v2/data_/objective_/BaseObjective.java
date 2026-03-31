package com.roguesmp.dungeon_v2.data_.objective_;

import java.util.Map;

public abstract class BaseObjective implements IObjective, PersistableObjective {

    protected ObjCallback callback;
    protected boolean completed = false;

    public void setCallback(ObjCallback callback){
        this.callback = callback;
    }


    protected void complete() {
        this.completed = true;
        if (callback != null) {
            callback.callback(this);
        }
    }

    @Override
    public Map<String, Object> serialize() {
        return Map.of(
                "completed", completed
        );
    }

    @Override
    public void deserialize(Map<String, Object> data) {
        this.completed = (boolean) data.getOrDefault("completed", false);
    }

}
