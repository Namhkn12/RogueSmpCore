package com.roguesmp.dungeon.behavior.bhv;

import com.roguesmp.dungeon.behavior.BehaviorData;
import com.roguesmp.dungeon.behavior.IBehavior;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;

public class ProtectorSpawner implements IBehavior {

    private final BehaviorData data;

    private int requireBreak = 3; //default require break setting
    private int brokenCount = 0;
    private boolean isComplete = false;

    public ProtectorSpawner(BehaviorData data) {
        this.data = data;
        Object raw = data.getParams().get("protector");
        if (raw != null) {
            this.requireBreak = ((Number) raw).intValue();
        }
    }

    @Override
    public boolean onSpawn(SpawnerSpawnEvent event) {
        return false;
    }

    @Override
    public void onTick() {

    }

    @Override
    public boolean onBreak(BlockBreakEvent event) {
        brokenCount++;
        event.getPlayer().sendMessage("Spawner đang được bảo vệ : " + brokenCount + "/" + requireBreak + " lần đập");
        return brokenCount == requireBreak;
    }

    @Override
    public BehaviorData getData() {
        return data;
    }
}
