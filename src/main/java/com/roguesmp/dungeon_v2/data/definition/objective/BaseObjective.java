package com.roguesmp.dungeon_v2.data.definition.objective;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Common runtime state shared by all objectives.
 */
public abstract class BaseObjective implements IObjective, PersistableObjective {

    protected ObjCallback callback;
    protected boolean completed;
    protected int score;

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

    public void setScore(int score) {
        this.score = score;
    }

    public void addScore(int plus) {
        this.score += plus;
    }

    public int getScore(){
        return score;
    }

    protected void complete() {
        this.completed = true;
        if (callback != null) {
            callback.callback(this);
        }
    }

    public CompletionScope getCompletionScope(){
        return CompletionScope.ROOM;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("completed", completed);
        data.put("score", score);
        return data;
    }

    @Override
    public void deserialize(Map<String, Object> data) {
        this.score = data.get("score") instanceof Number n ? n.intValue() : 0;
        this.completed = (boolean) data.getOrDefault("completed", false);
    }
}
