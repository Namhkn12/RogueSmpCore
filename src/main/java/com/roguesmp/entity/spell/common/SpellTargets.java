package com.roguesmp.entity.spell.common;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public final class SpellTargets {

    private SpellTargets() {
    }

    public static @Nullable Player playerTarget(LivingEntity owner) {
        if (owner instanceof Mob mob && mob.getTarget() instanceof Player player && player.isValid()) {
            return player;
        }
        return null;
    }

    public static Location centerOf(LivingEntity entity) {
        return entity.getLocation().add(0, entity.getHeight() * 0.5, 0);
    }

    public static double horizontalDistance(Location a, Location b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Horizontal unit vector pointing from {@code from} to {@code to}, falling back to {@code from}'s facing.
     */
    public static Vector horizontalDirection(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector()).setY(0);
        if (direction.lengthSquared() < 1.0E-4) {
            direction = from.getDirection().setY(0);
        }
        return direction.lengthSquared() < 1.0E-4 ? new Vector() : direction.normalize();
    }

    /**
     * Approximate launch velocity for a gravity/drag-affected projectile to land on {@code to}
     * after {@code flightTicks} ticks.
     */
    public static Vector ballisticVelocity(Location from, Location to, int flightTicks, double gravity, double drag) {
        double dragSum = (1 - Math.pow(drag, flightTicks)) / (1 - drag);
        Vector delta = to.toVector().subtract(from.toVector());
        double vy = (delta.getY() + 0.5 * gravity * flightTicks * flightTicks) / dragSum;
        return new Vector(delta.getX() / dragSum, vy, delta.getZ() / dragSum);
    }
}
