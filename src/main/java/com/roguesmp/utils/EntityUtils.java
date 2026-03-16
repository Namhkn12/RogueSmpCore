package com.roguesmp.utils;

import org.bukkit.Location;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class EntityUtils {
    public static double getMaxHealth(Attributable attributable) {
        AttributeInstance maxHealth = attributable.getAttribute(Attribute.MAX_HEALTH);
        return maxHealth == null ? 0 : maxHealth.getValue();
    }

    /**
     * Returns a List of LivingEntity excluding Players objects in the bounding box with the specified dimensions.
     *
     * @param loc       Location representing center of the bounding box
     * @param rx        distance from center to faces perpendicular to x-axis
     * @param ry        distance from center to faces perpendicular to y-axis
     * @param rz        distance from center to faces perpendicular to z-axis
     * @param predicate predicate to filter returned mobs
     * @return List of LivingEntity objects within the given bounding box matching the given predicate
     */
    public static List<LivingEntity> getNearbyMobs(Location loc, double rx, double ry, double rz, Predicate<LivingEntity> predicate) {
        return new ArrayList<>(loc.getWorld().getNearbyLivingEntities(loc, rx, ry, rz,
                entity -> entity.isValid() && !(entity instanceof Player) && predicate.test(entity)));
    }
}
