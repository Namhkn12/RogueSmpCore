package com.roguesmp.particle.path;

import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class ZigZagPath implements TravelPath {
    private final Location current;
    private final Vector direction;
    private final double speed, frequency, amplitude;

    public ZigZagPath(Location start, double speed, double frequency, double amplitude) {
        this.current = start.clone();
        this.direction = start.getDirection().normalize();
        this.speed = speed;
        this.frequency = frequency;
        this.amplitude = amplitude;
    }

    @Override
    public Location getLocation(int tick) {
        // Calculate the "side" vector (Right vector)
        Vector right = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
        double offset = FastMath.sin(tick * frequency) * amplitude;

        // Move forward + apply the side offset
        return current.add(direction.clone().multiply(speed)).add(right.multiply(offset));
    }

    @Override public Vector getDirection() { return direction; }
    @Override public boolean isFinished(int tick, Location loc) {
        return tick > 100 || loc.getBlock().getType().isSolid();
    }
}
