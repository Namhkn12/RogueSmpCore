package com.roguesmp.dungeon_v2.data.definition.spawner.behavior.impl;

import com.roguesmp.dungeon_v2.data.definition.spawner.behavior.BaseBehavior;
import com.roguesmp.dungeon_v2.data.definition.spawner.behavior.BehaviorData;
import com.roguesmp.dungeon_v2.utils.DungeonEcho;
import com.roguesmp.dungeon_v2.utils.SpawnLocationRazdon;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

public class WaveSpawner extends BaseBehavior {

    private final int waves;
    private int waveCount = 0;

    protected WaveSpawner(BehaviorData data) {
        super(data);
        this.waves = data.getInt("waves", 3);
    }

    @Override
    public boolean onBreak(Player player, Location location) {
        if(completed) return true;

        if(waveCount < waves){
            DungeonEcho.info(player,
                    "You must survive " + (waves - waveCount) + " more waves.");
            return false;
        }

        DungeonEcho.success(player,
                "All waves cleared. The spawner is now vulnerable.");
        return true;
    }

    @Override
    public boolean onSpawn(LivingEntity entity, List<Player> players, Location location) {
        if (completed) return true;

        waveCount++;
        if (waveCount >= waves) completed = true;

        int extra = Math.max(0, players.size() - 1);
        EntityType type = entity.getType();
        World world = location.getWorld();

        if (world == null) return true;

        for (int i = 0; i < extra; i++) {
            Location spawnLoc = SpawnLocationRazdon.getRandomLocation(location);
            world.spawnEntity(spawnLoc, type);
        }

        return true;
    }

    @Override
    public void reset() {
        super.reset();
        waveCount = 0;
    }
}
