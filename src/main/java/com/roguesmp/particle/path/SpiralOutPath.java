package com.roguesmp.particle.path;

import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class SpiralOutPath implements TravelPath {
    private final Location current;
    private final Vector forward;
    private final double speed, spiralGrowth;

    public SpiralOutPath(Location start, double speed, double spiralGrowth) {
        this.current = start.clone();
        this.forward = start.getDirection().normalize();
        this.speed = speed;
        this.spiralGrowth = spiralGrowth;
    }

    @Override
    public Location getLocation(int tick) {
        Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();
        Vector up = right.clone().crossProduct(forward).normalize();

        double radius = tick * spiralGrowth;
        double angle = tick * 0.5;

        Vector offset = right.multiply(FastMath.cos(angle) * radius).add(up.multiply(FastMath.sin(angle) * radius));
        return current.add(forward.clone().multiply(speed)).add(offset);
    }

    @Override public Vector getDirection() { return forward; }
    @Override public boolean isFinished(int tick, Location loc) { return tick > 150; }
}
