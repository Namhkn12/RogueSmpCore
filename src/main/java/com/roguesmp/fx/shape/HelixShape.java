package com.roguesmp.fx.shape;

import org.apache.commons.math3.util.FastMath;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/** A spiral of points winding around the local +Z axis. */
public final class HelixShape implements FxShape {

    private final List<Vector> points;

    public HelixShape(double radius, double length, double spacing, double turns) {
        List<Vector> pts = new ArrayList<>();
        int steps = (int) FastMath.max(1, length / spacing);

        for (int i = 0; i <= steps; i++) {
            double z = i * spacing;
            double angle = (z / length) * turns * 2 * FastMath.PI;
            pts.add(new Vector(FastMath.cos(angle) * radius, FastMath.sin(angle) * radius, z));
        }
        this.points = List.copyOf(pts);
    }

    @Override
    public List<Vector> points(int tick) {
        return points;
    }
}
