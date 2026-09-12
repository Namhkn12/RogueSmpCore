package com.roguesmp.utils;

import org.bukkit.Location;
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
}
