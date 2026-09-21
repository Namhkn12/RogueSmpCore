package com.roguesmp.utils;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

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
}
