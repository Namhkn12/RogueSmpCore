package com.roguesmp.fx.shape;

import org.apache.commons.math3.util.FastMath;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/** A hollow sphere of points evenly distributed on the surface via a Fibonacci lattice. */
public final class SphereShape implements FxShape {

    private final List<Vector> points;

    public SphereShape(double radius, int pointCount) {
        List<Vector> pts = new ArrayList<>(pointCount);
        double goldenAngle = FastMath.PI * (3.0 - FastMath.sqrt(5.0));

        for (int i = 0; i < pointCount; i++) {
            double y = 1 - (i / (double) (pointCount - 1)) * 2;
            double radiusAtY = FastMath.sqrt(1 - y * y);
            double theta = goldenAngle * i;

            double x = FastMath.cos(theta) * radiusAtY;
            double z = FastMath.sin(theta) * radiusAtY;

            pts.add(new Vector(x * radius, y * radius, z * radius));
        }
        this.points = List.copyOf(pts);
    }

    @Override
    public List<Vector> points(int tick) {
        return points;
    }
}
