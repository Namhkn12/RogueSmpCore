package com.roguesmp.particle.path;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.function.Supplier;

public class GuidedPath implements TravelPath {
    private final Location current;
    private final Supplier<Vector> input;
    private final double speed;
    private Vector lastDir;

    public GuidedPath(Location start, double speed, Supplier<Vector> input) {
        this.current = start.clone();
        this.speed = speed;
        this.input = input;
        this.lastDir = start.getDirection();
    }

    @Override
    public Location getLocation(int tick) {
        this.lastDir = input.get().normalize();
        return current.add(lastDir.clone().multiply(speed)).clone();
    }

    @Override public Vector getDirection() { return lastDir; }

    @Override public boolean isFinished(int tick, Location loc) {
        return tick > 200 || loc.getBlock().getType().isSolid();
    }
}
