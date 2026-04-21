package com.roguesmp.dungeon.data.definition.spawner.behavior.impl;

import com.roguesmp.dungeon.data.definition.spawner.behavior.BaseBehavior;
import com.roguesmp.dungeon.data.definition.spawner.behavior.BehaviorData;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Example json config
 *
 * {
 *   "spawnerId": "boss_guardian",
 *   "behaviors": [
 *     {
 *       "type": "protector",
 *       "requireBreak": 5
 *     }
 *   ]
 * }
 *
 * */
public class ProtectorSpawner extends BaseBehavior {

    private final int requireBreak;
    private int brokenCount = 0;

    public ProtectorSpawner(BehaviorData data) {
        super(data);
        this.requireBreak = data.getInt("requireBreak", 3);
    }

    @Override
    public boolean onBreak(Player player, Location location) {
        if (completed) return true;

        brokenCount++;
        int remaining = requireBreak - brokenCount;

        if (remaining > 0) {
            sendMessage(player, "The spawner shield active —  " + remaining + " break remaining.");
            return false;
        }

        completed = true;
        sendMessage(player, "Spawner defenses destroyed!");
        return true;
    }

    @Override
    public void reset() {
        super.reset();
        brokenCount = 0;
    }
}