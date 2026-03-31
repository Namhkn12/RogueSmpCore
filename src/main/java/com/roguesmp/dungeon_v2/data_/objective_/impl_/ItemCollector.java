package com.roguesmp.dungeon_v2.data_.objective_.impl_;

import com.roguesmp.dungeon_v2.data_.objective_.BaseObjective;
import com.roguesmp.dungeon_v2.data_.objective_.byevent_.IItemCollectAware;

import java.util.HashMap;
import java.util.Map;

public class ItemCollector extends BaseObjective implements IItemCollectAware {

    public static final String TYPE = "item_collector";
    //config
    private Map<String, Integer> require = new HashMap<>();
    //runtime state
    private Map<String, Integer> progress = new HashMap<>();

    public void setRequire(Map<String, Integer> require) {
        this.require = require;
    }

    @Override
    public void start() {

    }

    @Override
    public void finish() {

    }

    @Override
    public void onItemCollected(String itemId, int amount) {
        if (!require.containsKey(itemId)) return;

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
        Map<String, Object> data = new HashMap<>(super.serialize());
        data.put("type", TYPE);
        data.put("require", require);
        data.put("progress", progress);
        return data;
    }

    @Override
    public void deserialize(Map<String, Object> data) {
        super.deserialize(data);

        this.require = (Map<String, Integer>) data.getOrDefault("require", new HashMap<>());
        this.progress = (Map<String, Integer>) data.getOrDefault("progress", new HashMap<>());
    }

}
