package com.roguesmp.dungeon.objective_.impl;

import com.roguesmp.dungeon.objective_.ievento.IEntityDeadObjective;
import com.roguesmp.dungeon.objective_.obj_.KillObjective;
import com.roguesmp.dungeon.objective_.param.ObjectiveData;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashMap;
import java.util.Map;

public class MonsterSlayerObj extends KillObjective implements IEntityDeadObjective {
    public MonsterSlayerObj(ObjectiveData data) {
        this.required = data.getParams().getInt("count", 1);
        this.targetId = data.getParams().getStringList("mobs");
    }

    @Override
    public void onEntityDead(EntityDeathEvent e) {
        if (isValidTarget(e)) increment();
    }

    @Override
    public void start() {}

    @Override
    protected String getLabel() { return "☠ Tiêu diệt quái"; }

    @Override
    public int getScore() { return 50; }

    @Override
    public ObjectiveData getData() {
        Map<String, Object> params = new HashMap<>();
        params.put("count", required);
        if (!targetId.isEmpty()) params.put("mobs", targetId);
        return new ObjectiveData("monster_slayer", params);
    }
}
