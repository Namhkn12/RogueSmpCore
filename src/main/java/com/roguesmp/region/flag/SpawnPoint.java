package com.roguesmp.region.flag;

import org.bukkit.Location;
import org.bukkit.World;

public record SpawnPoint(double x, double y, double z, float yaw, float pitch) {

    public Location toLocation(World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }
}
