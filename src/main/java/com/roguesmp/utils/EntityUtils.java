package com.roguesmp.utils;

import com.roguesmp.constant.Keys;
import com.roguesmp.entity.BaseEntity;
import com.roguesmp.entity.EntityManager;
import com.roguesmp.tag.SmpTag;
import com.roguesmp.tag.Tags;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
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

    public static @Nullable LivingEntity getLivingEntityAtCursorExcludePlayers(Player player, double range) {
        return getLivingEntityAtCursorExcludePlayers(player, range, 0.425);
    }

    public static @Nullable LivingEntity getLivingEntityAtCursorExcludePlayers(Player player, double range, double hitboxSize) {
        return getLivingEntityAtCursor(player, range, entity -> entity.getType() != EntityType.PLAYER, hitboxSize);
    }

    public static @Nullable LivingEntity getLivingEntityAtCursor(Player player, double range, @Nullable Predicate<Entity> filter, double hitboxSize) {
        World world = player.getWorld();
        Location eyeLoc = player.getEyeLocation();
        RayTraceResult result = world.rayTrace(eyeLoc, eyeLoc.getDirection(), range, FluidCollisionMode.NEVER, true, hitboxSize,
                e -> (filter == null || filter.test(e))
                        // verify that the entity is actually ahead of the player (in case a large hitbox overlaps from behind)
                        && player.getLocation().getDirection().dot(e.getLocation().subtract(player.getLocation()).toVector()) > 0);
        // the raySize parameter changes the size of entity bounding boxes, so the entity may actually be outside the max range, hence the range check here
        if (result != null && result.getHitEntity() instanceof LivingEntity le && le.getLocation().distanceSquared(player.getLocation()) < range * range) {
            return le;
        }
        return null;
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

    public static boolean isFire(Entity entity) {
        return entity instanceof MagmaCube || entity instanceof Blaze;
    }

    public static boolean isElite(Entity entity) {
        return hasEntityTag(entity, Tags.ELITE);
    }

    public static boolean isBoss(Entity entity) {
        return hasEntityTag(entity, Tags.BOSS);
    }

    public static boolean isAngelic(Entity entity) {
        return hasEntityTag(entity, Tags.ANGELIC);
    }

    public static boolean isFriendly(Entity entity) {
        return hasEntityTag(entity, Tags.FRIENDLY);
    }

    private static boolean hasEntityTag(Entity entity, SmpTag<BaseEntity> tag) {
        BaseEntity base = getBaseEntity(entity);
        return base != null && tag.contains(base);
    }

    public static boolean isInStealth(Entity entity) {
        return EntityManager.getInstance().hasMetadata(entity, Keys.IN_STEALTH_META_KEY);
    }

    public static void clearAllMetadata(Entity entity) {
        EntityManager.getInstance().clearAllMetadata(entity);
    }

    public static void addMetadata(Entity entity, String key, Object value) {
        EntityManager.getInstance().addMetadata(entity, key, value);
    }

    public static void removeMetadata(Entity entity, String key) {
        EntityManager.getInstance().removeMetadata(entity, key);
    }

    public static boolean hasMetadata(Entity entity, String key) {
        return EntityManager.getInstance().hasMetadata(entity, key);
    }

    public static @Nullable Object getMetadataValue(Entity entity, String key) {
        return EntityManager.getInstance().getMetadataValue(entity, key);
    }

    public static @Nullable BaseEntity getBaseEntity(Entity entity) {
        String id = entity.getPersistentDataContainer().get(Keys.MOB_ID, PersistentDataType.STRING);
        if (id == null) return null;
        return EntityManager.getInstance().getBaseEntity(id);
    }

    public static @Nullable String getBaseEntityId(Entity entity) {
        return entity.getPersistentDataContainer().get(Keys.MOB_ID, PersistentDataType.STRING);
    }

    public static @Nullable LivingEntity getNearestMob(Location loc, double radius, Predicate<LivingEntity> predicate) {
        return getNearestMob(loc, getNearbyMobs(loc, radius, radius, radius, predicate));
    }

    public static @Nullable <T extends LivingEntity> T getNearestMob(Location loc, List<T> nearbyMobs) {
        boolean seen = false;
        T best = null;
        Comparator<T> comparator = Comparator.comparingDouble(e -> e.getLocation().distanceSquared(loc));
        for (T nearbyMob : nearbyMobs) {
            if (!seen || comparator.compare(nearbyMob, best) < 0) {
                seen = true;
                best = nearbyMob;
            }
        }
        return seen ? best : null;
    }
}
