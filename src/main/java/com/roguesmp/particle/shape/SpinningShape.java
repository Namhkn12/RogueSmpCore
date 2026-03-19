package com.roguesmp.particle.shape;

import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class SpinningShape implements ParticleShape {
    private final ParticleShape innerShape;
    private final double rotationSpeed; // Radians per tick

    public SpinningShape(ParticleShape inner, double rotationSpeed) {
        this.innerShape = inner;
        this.rotationSpeed = rotationSpeed;
    }

    @Override
    public List<Vector> getPoints(int tick) {
        // Calculate current rotation angle
        double theta = tick * rotationSpeed;
        double cos = Math.cos(theta);
        double sin = Math.sin(theta);

        List<Vector> originalPoints = innerShape.getPoints(tick);
        List<Vector> rotatedPoints = new ArrayList<>();

        for (Vector v : originalPoints) {
            // Apply 2D Rotation Matrix around the Z-axis
            // x' = x cos(theta) - y sin(theta)
            // y' = x sin(theta) + y cos(theta)
            double x = v.getX() * cos - v.getY() * sin;
            double y = v.getX() * sin + v.getY() * cos;

            rotatedPoints.add(new Vector(x, y, v.getZ()));
        }

        return rotatedPoints;
    }
}
