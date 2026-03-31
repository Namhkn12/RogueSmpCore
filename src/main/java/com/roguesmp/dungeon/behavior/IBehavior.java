package com.roguesmp.dungeon.behavior;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;

public interface IBehavior {
    boolean onSpawn(SpawnerSpawnEvent event);
    void onTick();
    boolean onBreak();
    BehaviorData getData();
}
