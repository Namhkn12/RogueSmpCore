package com.roguesmp.dungeon_v2.data.definition.objective.impl;

import com.roguesmp.dungeon_v2.data.definition.objective.BaseObjective;
import com.roguesmp.dungeon_v2.data.definition.objective.IProgressable;
import com.roguesmp.dungeon_v2.data.definition.objective.event.IEntityKillAware;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Objective completed after killing a target number of matching mobs.
 */
public class MonsterHunter extends BaseObjective implements IProgressable, IEntityKillAware {

    public static final String TYPE = "monster_hunter";

    private int require = 1;
    private int count;
    private List<String> targetIds = new ArrayList<>();

    public MonsterHunter() {
    }

    public int getRequire() {
        return require;
    }

    public void setRequire(int require) {
        this.require = require;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<String> getTargetIds() {
        return targetIds;
    }

    public void setTargetIds(List<String> targetIds) {
        this.targetIds = targetIds != null ? new ArrayList<>(targetIds) : new ArrayList<>();
    }

    @Override
    public void start() {
    }

    @Override
    public void finish() {
    }

    @Override
    public void progress() {
        count++;
        if (count >= require && !completed) {
            complete();
        }
    }

    @Override
    public void onEntityKilled(String mobId) {
        if (isValidTarget(mobId)) {
            progress();
        }
    }

    private boolean isValidTarget(String mobId) {
        return targetIds.isEmpty() || targetIds.contains(mobId);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>(super.serialize());
        data.put("type", TYPE);
        data.put("require", require);
        data.put("count", count);
        data.put("targets", new ArrayList<>(targetIds));
        return data;
    }

    @Override
    public void deserialize(Map<String, Object> data) {
        super.deserialize(data);
        this.require = data.get("require") instanceof Number number ? number.intValue() : 1;
        this.count = data.get("count") instanceof Number number ? number.intValue() : 0;
        this.targetIds = new ArrayList<>();
        Object rawTargets = data.get("targets");
        if (rawTargets instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof String value) {
                    this.targetIds.add(value);
                }
            }
        }
    }
}
