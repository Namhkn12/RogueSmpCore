package com.roguesmp.utils;

import com.destroystokyo.paper.ParticleBuilder;
import com.roguesmp.particle.ParticleShape;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.List;

public class ParticleUtils {

    /**
     * Spawns multiple particle types for every point defined in a shape.
     * The rotation matrix is calculated once per call to maximize performance.
     *
     * @param loc The center location (direction determines shape orientation)
     * @param shape The geometry blueprint
     * @param builders A list of particle builders to be spawned at every coordinate
     * @param tick The current animation tick
     */
    public static void spawnShape(Location loc, ParticleShape shape, List<ParticleBuilder> builders, int tick) {
        List<Vector> offsets = shape.getOffsets(tick);
        if (offsets.isEmpty() || builders.isEmpty()) return;

        // Use standard Minecraft angles
        double yaw = FastMath.toRadians(-loc.getYaw());
        double pitch = FastMath.toRadians(-loc.getPitch());

        double cosYaw = FastMath.cos(yaw);
        double sinYaw = FastMath.sin(yaw);
        double cosPitch = FastMath.cos(pitch);
        double sinPitch = FastMath.sin(pitch);

        double originX = loc.getX();
        double originY = loc.getY();
        double originZ = loc.getZ();
        World world = loc.getWorld();

        for (Vector v : offsets) {
            double x = v.getX();
            double y = v.getY();
            double z = v.getZ();

            // 1. Rotate Pitch (around the X-axis)
            // This tilts the Z-axis up and down
            double z1 = z * cosPitch - y * sinPitch;
            double y1 = z * sinPitch + y * cosPitch;

            // 2. Rotate Yaw (around the Y-axis)
            // This swings the tilted Z-axis left and right
            double finalZ = z1 * cosYaw - x * sinYaw;
            double finalX = z1 * sinYaw + x * cosYaw;
            double finalY = y1;

            for (ParticleBuilder builder : builders) {
                builder.location(world, originX + finalX, originY + finalY, originZ + finalZ).spawn();
            }
        }
    }

    public static void spawnShape(Location loc, ParticleShape shape, ParticleBuilder particleBuilder, int tick) {
        spawnShape(loc, shape, Collections.singletonList(particleBuilder), tick);
    }
}
