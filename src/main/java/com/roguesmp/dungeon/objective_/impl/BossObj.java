package com.roguesmp.dungeon.objective_.impl;

import com.roguesmp.dungeon.objective_.ievento.IEntityDeadObjective;
import com.roguesmp.dungeon.objective_.obj_.KillObjective;
import com.roguesmp.dungeon.objective_.param.ObjectiveData;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Map;

public class BossObj extends KillObjective implements IEntityDeadObjective {

    public BossObj(ObjectiveData data) {
        this.required = 1;
        this.targetId = data.getParams().getStringList("bossId");
    }

    @Override
    public void onEntityDead(EntityDeathEvent e) {
        if (isValidTarget(e)) increment();
    }

    @Override
    protected String getLabel() { return "☠ Tiêu diệt Boss"; }

    @Override
    public int getScore() { return 200; }

    @Override
    public void start() {}

    @Override
    public ObjectiveData getData() {
        return new ObjectiveData("boss_kill", Map.of("bossId", targetId));
    }
}
