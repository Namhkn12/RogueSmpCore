package com.roguesmp.dungeon_core.room;

import com.roguesmp.dungeon_core.FeatureContext;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.List;

public record RoomEffectContext<S>(FeatureContext<?> run, BoundingBox bounds, int seconds, S state) {

    public List<Player> players() {
        return run.onlinePlayers();
    }
}
