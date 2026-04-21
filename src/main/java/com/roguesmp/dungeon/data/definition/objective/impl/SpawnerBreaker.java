package com.roguesmp.dungeon.data.definition.objective.impl;

import com.roguesmp.dungeon.data.definition.objective.BaseObjective;
import com.roguesmp.dungeon.data.definition.objective.IDisplayable;
import com.roguesmp.dungeon.data.definition.objective.IProgressable;
import com.roguesmp.dungeon.data.definition.objective.event.IBlockBreakAware;
import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Objective completed after a number of spawners have been broken.
 */
public class SpawnerBreaker extends BaseObjective implements IProgressable, IDisplayable, IBlockBreakAware {
    public static final String TYPE = "spawner_breaker";

    private int require = 3;
    private int count;

    public SpawnerBreaker() {
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
    public void onBlockBreak(Material type) {
        if (type == Material.SPAWNER) {
            progress();
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>(super.serialize());
        data.put("type", TYPE);
        data.put("require", require);
        data.put("count", count);
        return data;
    }

    @Override
    public void deserialize(Map<String, Object> data) {
        super.deserialize(data);
        this.require = data.get("require") instanceof Number number ? number.intValue() : 3;
        this.count = data.get("count") instanceof Number number ? number.intValue() : 0;
    }

    @Override
    public List<String> getScoreBoardLine() {
        String progress = completed
                ? "§a✔ Break Spawners §f" + require + "§7/§f" + require
                : "§7Break Spawners §f" + count + "§7/§f" + require;
        return List.of(progress);
    }
}
