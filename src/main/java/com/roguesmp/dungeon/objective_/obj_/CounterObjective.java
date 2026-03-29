package com.roguesmp.dungeon.objective_.obj_;

import com.roguesmp.dungeon.objective_.BaseObjective;

import java.util.List;
import java.util.Map;

public abstract class CounterObjective extends BaseObjective {
    protected int count = 0;
    protected int required;

    protected void increment() {
        if (completed) return;
        count++;
        if (count >= required) onComplete();
    }

    @Override
    public String getMessage() {
        return getLabel() + ": " + count + "/" + required;
    }

    @Override
    public List<String> getMessageScoreBoard() {
        return List.of(getLabel(), count + "/" + required + (completed ? " ✓" : ""));
    }

    protected abstract String getLabel();

    @Override
    public Map<String, Object> exportProgress() {
        return Map.of("count", count, "completed", completed);
    }

    @Override
    public void importProgress(Map<String, Object> progress) {
        this.count = ((Number) progress.getOrDefault("count", 0)).intValue();
        this.completed = (Boolean) progress.getOrDefault("completed", false);
    }
}