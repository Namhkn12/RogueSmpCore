package com.roguesmp.utils;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class LocationUtils {
    /**
     * Return a normalized direction vector.
     */
    public static Vector getDirectionTo(Location to, Location from) {
        Vector vFrom = from.toVector();
        Vector vTo = to.toVector();
        Vector diff = vTo.subtract(vFrom);
        Vector normalized = diff.normalize();
        if (!Double.isFinite(normalized.getX())) {
            return new Vector(0, 1, 0);
        }
        return normalized;
    }

    public static Location getHeightLocation(Entity entity, double heightMultiplier) {
        return entity.getLocation().add(0, entity.getHeight() * heightMultiplier, 0);
    }

    public static Location getHalfHeightLocation(Entity entity) {
        return getHeightLocation(entity, 0.5);
    }

    // Player eye height 1.62 when not sneaking
    public static Location getHalfEyeLocation(LivingEntity entity) {
        return entity.getLocation().add(0, entity.getEyeHeight() / 2, 0);
    }

    public static Location randomOffsetXZ(Location center, double minRadius, double maxRadius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = minRadius + random.nextDouble() * (maxRadius - minRadius);
        return center.clone().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
    }

    public static Location randomStandableNear(Location center) {
        for (int i = 0; i < 10; i++) {
            Location candidate = randomOffsetXZ(center, 1.5, 5.0);
            if (candidate.getBlock().isPassable() && candidate.clone().add(0, 1, 0).getBlock().isPassable()) {
                return candidate;
            }
        }
        return randomOffsetXZ(center, 0, 2.0);
    }
}
