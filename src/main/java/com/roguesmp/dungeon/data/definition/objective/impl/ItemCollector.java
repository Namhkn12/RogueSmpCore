package com.roguesmp.dungeon.data.definition.objective.impl;

import com.roguesmp.dungeon.data.definition.objective.BaseObjective;
import com.roguesmp.dungeon.data.definition.objective.IDisplayable;
import com.roguesmp.dungeon.data.definition.objective.IProgressable;
import com.roguesmp.dungeon.data.definition.objective.event.IItemCollectAware;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Objective completed when all required item counts are collected.
 */
public class ItemCollector extends BaseObjective implements IDisplayable, IProgressable, IItemCollectAware {

    public static final String TYPE = "item_collector";

    private String targetItemId;
    private int require = 1;
    private int count;

    public ItemCollector() {}

    public String getTargetItemId() { return targetItemId; }
    public void setTargetItemId(String targetItemId) { this.targetItemId = targetItemId; }

    public int getRequire() { return require; }
    public void setRequire(int require) { this.require = require; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    @Override
    public void start() {}

    @Override
    public void finish() {}

    @Override
    public void onItemCollected(String itemId, int amount) {
        if (targetItemId != null && !targetItemId.equals(itemId)) return;
        if (completed) return;

        count = Math.min(count + amount, require);
        if (count >= require) {
            complete();
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>(super.serialize());
        data.put("type", TYPE);
        data.put("target", targetItemId);
        data.put("require", require);
        data.put("count", count);
        return data;
    }

    @Override
    public void deserialize(Map<String, Object> data) {
        super.deserialize(data);
        this.targetItemId = data.get("target") instanceof String s ? s : null;
        this.require = data.get("require") instanceof Number n ? n.intValue() : 1;
        this.count = data.get("count") instanceof Number n ? n.intValue() : 0;
    }

    @Override
    public List<String> getScoreBoardLine() {
        String label = targetItemId == null ? "Collect Items" : "Collect " + targetItemId;
        String line = completed
                ? "§a✔ " + label + " §f" + require + "§7/§f" + require
                : "§7" + label + " §f" + count + "§7/§f" + require;
        return List.of(line);
    }

    @Override
    public void progress() {
        count++;
        if (count >= require && !completed) {
            complete();
        }
    }
}
