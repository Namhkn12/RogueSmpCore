package com.roguesmp.dungeon_v2.definition.objective;

import java.util.Map;

/**
 * Common runtime state shared by all objectives.
 */
public abstract class BaseObjective implements IObjective, PersistableObjective {

    protected ObjCallback callback;
    protected boolean completed;

    public ObjCallback getCallback() {
        return callback;
    }

    public void setCallback(ObjCallback callback) {
        this.callback = callback;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    protected void complete() {
        this.completed = true;
        if (callback != null) {
            callback.callback(this);
        }
    }

    @Override
    public Map<String, Object> serialize() {
        return Map.of("completed", completed);
    }

    @Override
    public void deserialize(Map<String, Object> data) {
        this.completed = (boolean) data.getOrDefault("completed", false);
    }
}
