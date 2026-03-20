package com.roguesmp.particle.shape;

import org.apache.commons.math3.util.FastMath;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class StarShape implements ParticleShape {
    private final double outerRadius, innerRadius;
    private final int points = 5;

    public StarShape(double outerRadius, double innerRadius) {
        this.outerRadius = outerRadius;
        this.innerRadius = innerRadius;
    }

    @Override
    public List<Vector> getPoints(int tick) {
        List<Vector> list = new ArrayList<>();
        // 10 total vertices (5 outer, 5 inner)
        for (int i = 0; i < points * 2; i++) {
            double angle = i * Math.PI / points;
            double r = (i % 2 == 0) ? outerRadius : innerRadius;
            list.add(new Vector(FastMath.cos(angle) * r, FastMath.sin(angle) * r, 0));
        }
        return list;
    }
}