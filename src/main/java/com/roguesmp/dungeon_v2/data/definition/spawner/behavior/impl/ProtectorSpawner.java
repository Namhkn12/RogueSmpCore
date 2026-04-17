package com.roguesmp.dungeon_v2.data.definition.spawner.behavior.impl;

import com.roguesmp.dungeon_v2.data.definition.spawner.behavior.BaseBehavior;
import com.roguesmp.dungeon_v2.data.definition.spawner.behavior.BehaviorData;
import org.bukkit.Location;
import org.bukkit.entity.Player;

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
            sendMessage(player, "Spawner được bảo vệ — còn " + remaining + " lần.");
            return false;
        }

        completed = true;
        sendMessage(player, "Lớp bảo vệ đã bị phá!");
        return true;
    }

    @Override
    public void reset() {
        super.reset();
        brokenCount = 0;
    }
}