package com.roguesmp.particle;

import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class LineShape implements ParticleShape {
    private final List<Vector> points = new ArrayList<>();

    public LineShape(double length, double spacing) {
        for (double d = 0; d <= length; d += spacing) {
            // In our system, Z+ is the forward direction
            points.add(new Vector(0, 0, d));
        }
    }

    @Override
    public List<Vector> getOffsets(int tick) {
        return points;
    }

}
