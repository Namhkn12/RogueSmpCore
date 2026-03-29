package com.roguesmp.dungeon.objective_.impl;

import com.roguesmp.dungeon.objective_.param.ObjectiveData;
import com.roguesmp.dungeon.objective_.ievento.IBlockBreakObjective;
import com.roguesmp.dungeon.objective_.obj_.CounterObjective;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Map;

public class SpawnerBreakObj extends CounterObjective implements IBlockBreakObjective {

    public SpawnerBreakObj(ObjectiveData data) {
        this.required = data.getParams().getInt("count", 1);
    }

    @Override
    public void onBlockBreak(BlockBreakEvent e) {
        increment();
    }

    @Override
    public void start() {}

    @Override
    protected String getLabel() { return "☠ Phá spawner"; }

    @Override
    public int getScore() { return 20; }

    @Override
    public ObjectiveData getData() {
        return new ObjectiveData("spawner_break", Map.of("count", required));
    }
}
