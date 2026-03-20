package com.roguesmp.particle.path;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public class GravityPath implements TravelPath {
    private final Location current;
    private final Vector velocity;
    private final double gravity = 0.04;

    public GravityPath(Location start, double initialSpeed) {
        this.current = start.clone();
        this.velocity = start.getDirection().normalize().multiply(initialSpeed);
    }

    @Override
    public Location getLocation(int tick) {
        velocity.setY(velocity.getY() - gravity); // Apply gravity to velocity
        return current.add(velocity);
    }

    @Override public Vector getDirection() { return velocity.clone().normalize(); }
    @Override public boolean isFinished(int tick, Location loc) {
        return loc.getY() < 0 || loc.getBlock().getType().isSolid();
    }
}
