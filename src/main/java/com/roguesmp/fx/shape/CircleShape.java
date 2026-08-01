package com.roguesmp.fx.shape;

import org.apache.commons.math3.util.FastMath;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/** A flat circle of evenly spaced points on the local XZ plane. */
public final class CircleShape implements FxShape {

    private final List<Vector> points;

    public CircleShape(double radius, int pointCount) {
        List<Vector> pts = new ArrayList<>(pointCount);
        for (int i = 0; i < pointCount; i++) {
            double angle = (2 * FastMath.PI / pointCount) * i;
            pts.add(new Vector(FastMath.cos(angle) * radius, 0, FastMath.sin(angle) * radius));
        }
        this.points = List.copyOf(pts);
    }

    @Override
    public List<Vector> points(int tick) {
        return points;
    }
}
