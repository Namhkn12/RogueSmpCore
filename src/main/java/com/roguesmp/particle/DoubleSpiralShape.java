package com.roguesmp.particle;

import org.bukkit.util.Vector;
import java.util.List;

/**
 * Creates two spiraling points that rotate around the forward axis (Z-axis).
 * @param radius The distance from the center axis.
 * @param speed  The angular velocity (rotation per tick).
 */
public record DoubleSpiralShape(double radius, double speed) implements ParticleShape {

    @Override
    public List<Vector> getOffsets(int tick) {
        double angle = tick * speed;

        // Calculate point 1
        double x1 = Math.cos(angle) * radius;
        double y1 = Math.sin(angle) * radius;

        // Calculate point 2 (180 degrees or PI radians offset)
        double x2 = Math.cos(angle + Math.PI) * radius;
        double y2 = Math.sin(angle + Math.PI) * radius;

        return List.of(
                new Vector(x1, y1, 0),
                new Vector(x2, y2, 0)
        );
    }
}
