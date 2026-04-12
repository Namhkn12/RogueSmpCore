package com.roguesmp.dungeon_v2.data.definition.objective.impl;

import com.roguesmp.dungeon_v2.data.definition.objective.BaseObjective;
import com.roguesmp.dungeon_v2.data.definition.objective.IDisplayable;
import com.roguesmp.dungeon_v2.data.definition.objective.IProgressable;
import com.roguesmp.dungeon_v2.data.definition.objective.event.IEntityKillAware;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Objective completed after killing a target number of matching mobs.
 */
public class MonsterHunter extends BaseObjective implements IProgressable, IDisplayable, IEntityKillAware {

    public static final String TYPE = "monster_hunter";

    private int require = 1;
    private int count;
    private String targetId;

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

    public String getTargetId() { return targetId; }

    public void setTargetId(String targetId) { this.targetId = targetId; }

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
        return targetId == null || targetId.equals(mobId);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>(super.serialize());
        data.put("type", TYPE);
        data.put("require", require);
        data.put("count", count);
        data.put("target", targetId);
        return data;
    }

    @Override
    public void deserialize(Map<String, Object> data) {
        super.deserialize(data);
        this.require = data.get("require") instanceof Number n ? n.intValue() : 1;
        this.count = data.get("count") instanceof Number n ? n.intValue() : 0;
        this.targetId = data.get("target") instanceof String s ? s : null;
    }

    @Override
    public List<String> getScoreBoardLine() {
        String label = targetId == null ? "Kill Mobs" : "Kill " + targetId;
        String progress = completed
                ? "§a✔ " + label + " §f" + require + "§7/§f" + require
                : "§7" + label + " §f" + count + "§7/§f" + require;
        return List.of(progress);
    }
}
