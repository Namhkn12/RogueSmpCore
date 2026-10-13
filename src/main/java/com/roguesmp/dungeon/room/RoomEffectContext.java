package com.roguesmp.dungeon.room;

import com.roguesmp.dungeon.FeatureContext;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.List;

public record RoomEffectContext<S>(FeatureContext<?> run, BoundingBox bounds, int seconds, S state) {

    public List<Player> players() {
        return run.onlinePlayers();
    }
}
