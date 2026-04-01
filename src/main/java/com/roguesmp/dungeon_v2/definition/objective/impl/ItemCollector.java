package com.roguesmp.dungeon_v2.definition.objective.impl;

import com.roguesmp.dungeon_v2.definition.objective.BaseObjective;
import com.roguesmp.dungeon_v2.definition.objective.event.IItemCollectAware;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Objective completed when all required item counts are collected.
 */
public class ItemCollector extends BaseObjective implements IItemCollectAware {

    public static final String TYPE = "item_collector";

    private Map<String, Integer> require = new LinkedHashMap<>();
    private Map<String, Integer> progress = new LinkedHashMap<>();

    public ItemCollector() {
    }

    public Map<String, Integer> getRequire() {
        return require;
    }

    public void setRequire(Map<String, Integer> require) {
        this.require = require != null ? new LinkedHashMap<>(require) : new LinkedHashMap<>();
    }

    public Map<String, Integer> getProgressMap() {
        return progress;
    }

    public void setProgress(Map<String, Integer> progress) {
        this.progress = progress != null ? new LinkedHashMap<>(progress) : new LinkedHashMap<>();
    }

    @Override
    public void start() {
    }

    @Override
    public void finish() {
    }

    @Override
    public void onItemCollected(String itemId, int amount) {
        if (!require.containsKey(itemId)) {
            return;
        }

        int current = progress.getOrDefault(itemId, 0);
        int need = require.get(itemId);
        int newValue = Math.min(current + amount, need);
        progress.put(itemId, newValue);

        checkComplete();
    }

    private void checkComplete() {
        for (Map.Entry<String, Integer> entry : require.entrySet()) {
            int current = progress.getOrDefault(entry.getKey(), 0);
            if (current < entry.getValue()) {
                return;
            }
        }

        if (!completed) {
            complete();
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>(super.serialize());
        data.put("type", TYPE);
        data.put("require", new LinkedHashMap<>(require));
        data.put("progress", new LinkedHashMap<>(progress));
        return data;
    }

    @Override
    public void deserialize(Map<String, Object> data) {
        super.deserialize(data);
        setRequire(readIntMap(data.get("require")));
        setProgress(readIntMap(data.get("progress")));
    }

    private Map<String, Integer> readIntMap(Object rawValue) {
        Map<String, Integer> values = new LinkedHashMap<>();
        if (!(rawValue instanceof Map<?, ?> rawMap)) {
            return values;
        }

        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() instanceof String key && entry.getValue() instanceof Number value) {
                values.put(key, value.intValue());
            }
        }
        return values;
    }
}
