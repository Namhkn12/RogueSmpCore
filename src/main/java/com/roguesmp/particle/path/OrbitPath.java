package com.roguesmp.particle.path;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public class OrbitPath implements TravelPath {
    private final Location center;
    private final double radius;

    public OrbitPath(Location center, double radius) { this.center = center; this.radius = radius; }

    @Override
    public Location getLocation(int tick) {
        double angle = tick * 0.1;
        return center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
    }

    @Override public Vector getDirection() { return new Vector(0, 1, 0); }
    @Override public boolean isFinished(int tick, Location loc) { return tick > 100; }
}
