package com.roguesmp.dungeon;

import com.roguesmp.dungeon.script.DataValue;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record FeatureContext<S>(DungeonManager manager, DungeonInstance instance, String featureId, DungeonFeature<S> feature, S state) {

    public @Nullable World world() {
        return manager.worldOf(instance);
    }

    public List<Player> onlinePlayers() {
        World world = world();
        return world == null ? List.of() : world.getPlayers();
    }

    public void fire(DungeonEvent event) {
        manager.fire(instance, event);
    }

    public void signal(String id, int amount, @Nullable Player source) {
        fire(new DungeonEvent.Custom(id, amount, source));
    }

    public void signal(String id, int amount, @Nullable Player source, Map<String, DataValue> data) {
        fire(new DungeonEvent.Custom(id, amount, source, data));
    }

    public void end(String reason) {
        manager.end(instance, reason);
    }

    public void save() {
        manager.save(instance);
    }
}
