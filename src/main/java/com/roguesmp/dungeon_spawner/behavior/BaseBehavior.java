package com.roguesmp.dungeon_spawner.behavior;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class BaseBehavior {

    protected final BehaviorData data;
    protected boolean completed = false;

    protected BaseBehavior(BehaviorData data) {
        this.data = data;
    }

    public boolean onBreak(Player player, Location location) { return true; }
    public boolean onSpawn(LivingEntity entity, List<Player> players, Location location) { return true; }
    public void onTick() { }
    public void onPlayerEnter(Player player) { }

    public boolean isCompleted() { return completed; }
    public BehaviorData getData() { return data; }

    public void reset() {
        completed = false;
    }

    protected void sendMessage(Player player, String message) {
        player.sendMessage("§8[§6Dungeon§8] §f" + message);
    }
}
