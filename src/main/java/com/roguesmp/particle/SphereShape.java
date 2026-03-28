package com.roguesmp.particle;

import org.bukkit.util.Vector;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Creates a hollow sphere of points using a Fibonacci Lattice for even distribution.
 */
public final class SphereShape implements ParticleShape {
    private final List<Vector> points = new ArrayList<>();

    /**
     * @param radius  The radius of the sphere.
     * @param density The number of points to distribute on the surface.
     */
    public SphereShape(double radius, int density) {

        double phi = Math.PI * (3.0 - Math.sqrt(5.0)); // Golden angle in radians

        for (int i = 0; i < density; i++) {
            double y = 1 - (i / (double) (density - 1)) * 2; // y goes from 1 to -1
            double radiusAtY = Math.sqrt(1 - y * y); // radius at distance y from center

            double theta = phi * i; // Golden angle increment

            double x = Math.cos(theta) * radiusAtY;
            double z = Math.sin(theta) * radiusAtY;

            points.add(new Vector(x * radius, y * radius, z * radius));
        }
    }

    @Override
    public List<Vector> getOffsets(int tick) {
        return points;
    }

}
