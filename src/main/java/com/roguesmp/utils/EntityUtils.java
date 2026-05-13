package com.roguesmp.utils;

import com.roguesmp.entity.EntityManager;
import org.bukkit.Location;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class EntityUtils {
    public static double getMaxHealth(Attributable attributable) {
        AttributeInstance maxHealth = attributable.getAttribute(Attribute.MAX_HEALTH);
        return maxHealth == null ? 0 : maxHealth.getValue();
    }

    public static void setHealthPercent(LivingEntity living, double value) {
        double maxHp = getMaxHealth(living);
        living.setHealth(Math.clamp(maxHp * value, 0d, maxHp));
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

    public static void healPercent(LivingEntity living, double percentToHeal) {
        double maxHp = getMaxHealth(living);
        double toHeal = maxHp * (percentToHeal / 100);
        living.heal(toHeal);
    }

    public static double getAttributeOrDefault(Attributable entity, Attribute attribute, double def) {
        AttributeInstance attr = entity.getAttribute(attribute);
        return attr == null ? def : attr.getValue();
    }

    public static double healthScalingCoef(int playerCount, double x, double y) {
        if (playerCount < 1) {
            return 1;
        }
        double scalingCoef = 0;
        // calculates smallest scaling increase first. largest scaling increase last.
        while (playerCount > 0) {
            double iterCoef = Math.pow(x, Math.pow(playerCount - 1, y));
            scalingCoef += iterCoef;
            playerCount--;
        }
        return scalingCoef;
    }

    public static boolean isAquatic(Entity entity) {
        return entity instanceof WaterMob || entity instanceof Guardian || entity instanceof Turtle;
    }

    public static boolean isArthropod(Entity entity) {
        return entity instanceof Spider || entity instanceof Silverfish || entity instanceof Endermite || entity instanceof Bee;
    }

    public static boolean isUndead(Entity entity) {
        return entity instanceof AbstractSkeleton || entity instanceof Zombie ||
                entity instanceof Wither || entity instanceof Phantom;
    }

    public void clearAllMetadata(Entity entity) {
        EntityManager.getInstance().clearAllMetadata(entity);
    }

    public void addMetadata(Entity entity, String key, Object value) {
        EntityManager.getInstance().addMetadata(entity, key, value);
    }

    public void removeMetadata(Entity entity, String key) {
        EntityManager.getInstance().removeMetadata(entity, key);
    }

    public boolean hasMetadata(Entity entity, String key) {
        return EntityManager.getInstance().hasMetadata(entity, key);
    }

    public @Nullable Object getMetadataValue(Entity entity, String key) {
        return EntityManager.getInstance().getMetadataValue(entity, key);
    }
}
