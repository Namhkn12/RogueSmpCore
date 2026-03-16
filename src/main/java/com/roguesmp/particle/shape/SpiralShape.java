package com.roguesmp.particle.shape;

import org.apache.commons.math3.util.FastMath;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class SpiralShape implements ParticleShape {

    private final List<Vector> points = new ArrayList<>();

    public SpiralShape(double radius, double height, int turns, int pointsCount) {

        for (int i = 0; i < pointsCount; i++) {

            double t = (double) i / pointsCount;
            double angle = t * turns * Math.PI * 2;

            double x = radius * FastMath.cos(angle);
            double z = radius * FastMath.sin(angle);
            double y = height * t;

            points.add(new Vector(x, y, z));
        }
    }

    @Override
    public List<Vector> getPoints() {
        return points;
    }
}
