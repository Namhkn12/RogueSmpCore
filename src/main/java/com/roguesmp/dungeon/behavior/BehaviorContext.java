package com.roguesmp.dungeon.behavior;

import com.roguesmp.dungeon.data.Spawner;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntitySnapshot;

public class BehaviorContext {
    private final CreatureSpawner spawner;
    private final Spawner data;
    private final EntitySnapshot snapshot;

    public BehaviorContext(CreatureSpawner spawner, Spawner data, EntitySnapshot snapshot) {
        this.spawner = spawner;
        this.data = data;
        this.snapshot = snapshot;
    }
}
