package com.roguesmp.dungeon_v2.presentation;

import com.roguesmp.dungeon_v2.presentation.particle.DungeonParticle;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;

public class ParticleManager {

    public void spawn(Player player, Location location, DungeonParticle data) {
        if (data == null || location == null) return;
        if (player == null || !player.isOnline()) return;

        player.spawnParticle(
                data.getParticle(),
                location,
                data.getCount(),
                data.getOffsetX(),
                data.getOffsetY(),
                data.getOffsetZ(),
                data.getExtra()
        );
    }

    public void spawn(Collection<Player> players, Location location, DungeonParticle data) {
        if (data == null) return;
        players.forEach(p -> spawn(p, location, data));
    }
}