package com.roguesmp.particle.shape;

import org.apache.commons.math3.util.FastMath;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class AtomShape implements ParticleShape {
    private final double radius;

    public AtomShape(double radius) { this.radius = radius; }

    @Override
    public List<Vector> getPoints(int tick) {
        List<Vector> points = new ArrayList<>();
        double angle = tick * 0.2;

        // Ring 1 (Vertical-ish)
        points.add(new Vector(Math.cos(angle) * radius, FastMath.sin(angle) * radius, 0));
        // Ring 2 (Tilted 60 degrees)
        points.add(new Vector(Math.cos(angle) * radius, 0, FastMath.sin(angle) * radius));
        // Ring 3 (Tilted -60 degrees)
        points.add(new Vector(0, Math.cos(angle) * radius, FastMath.sin(angle) * radius));

        return points;
    }
}
