package com.roguesmp.fx.shape;

import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/** A straight line of evenly spaced points from the origin along the local +Z axis. */
public final class LineShape implements FxShape {

    private final List<Vector> points;

    public LineShape(double length, double spacing) {
        List<Vector> pts = new ArrayList<>();
        for (double d = 0; d <= length; d += spacing) {
            pts.add(new Vector(0, 0, d));
        }
        this.points = List.copyOf(pts);
    }

    @Override
    public List<Vector> points(int tick) {
        return points;
    }
}
