package com.roguesmp.dungeon_v2.data.definition.spawner.behavior;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class BaseBehavior implements IBehavior {

    protected final BehaviorData data;
    protected boolean completed = false;

    protected BaseBehavior(BehaviorData data) {
        this.data = data;
    }

    // Default no-op — chỉ override những gì behavior cần

    @Override public boolean onBreak(Player player, Location location)       { return true; }
    @Override public boolean onSpawn(LivingEntity entity, List<Player> players, Location location) { return true; }
    @Override public void onTick()                        { }
    @Override public void onPlayerEnter(Player player)    { }

    @Override public boolean isCompleted() { return completed; }
    @Override public BehaviorData getData() { return data; }

    @Override
    public void reset() {
        completed = false;
    }

    // ── Shared utils ─────────────────────────────────────────────────────────

    protected void sendMessage(Player player, String message) {
        player.sendMessage("§8[§6Dungeon§8] §f" + message);
    }
}