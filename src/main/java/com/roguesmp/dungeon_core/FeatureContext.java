package com.roguesmp.dungeon_core;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * What a feature hook gets: its own state and the run it belongs to.
 */
public record FeatureContext<S>(DungeonManager manager, DungeonInstance instance, String featureId, DungeonFeature<S> feature, S state) {

    public @Nullable World world() {
        return manager.worldOf(instance);
    }

    public List<Player> onlinePlayers() {
        World world = world();
        return world == null ? List.of() : world.getPlayers();
    }

    public void signal(DungeonSignal signal) {
        manager.signal(instance, signal);
    }

    public void signal(String id, int amount, @Nullable Player source) {
        signal(new DungeonSignal.Custom(id, amount, source));
    }

    public void signal(String id, int amount, @Nullable Player source, Map<String, String> data) {
        signal(new DungeonSignal.Custom(id, amount, source, data));
    }

    public void end(String reason) {
        manager.end(instance, reason);
    }

    /**
     * Saves the run, for state changes that must survive a crash.
     */
    public void save() {
        manager.save(instance);
    }
}
