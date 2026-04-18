package com.roguesmp.dungeon_v2.utils;

import org.bukkit.Location;
import org.bukkit.World;

public class SpawnLocationRazdon {
    public static Location getRandomLocation(Location center) {
        Razdon rng = Razdon.getInstance();
        World world = center.getWorld();
        if (world == null) return center;

        for (int i = 0; i < 10; i++) {
            Location candidate = rng.nextLocationXZ(center, 1.5, 5.0);
            if (candidate.getBlock().isPassable()
                    && candidate.clone().add(0, 1, 0).getBlock().isPassable()) {
                return candidate;
            }
        }

        return rng.nextLocationXZ(center, 0, 2.0); // fallback
    }
}
