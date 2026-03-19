package com.roguesmp.particle.shape;

import org.apache.commons.math3.util.FastMath;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.List;

// A single point that rotates around the center
public class RotatingPoint implements ParticleShape {
    private final double radius, rotationSpeed, offset;

    public RotatingPoint(double radius, double rotationSpeed, double offset) {
        this.radius = radius;
        this.rotationSpeed = rotationSpeed;
        this.offset = offset;
    }

    @Override
    public List<Vector> getPoints(int tick) {
        double angle = (tick * rotationSpeed) + offset;
        return Collections.singletonList(new Vector(
                FastMath.cos(angle) * radius,
                FastMath.sin(angle) * radius,
                0
        ));
    }
}
