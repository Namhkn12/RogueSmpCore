package com.roguesmp.utils;

import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable position that refers to its world by key, so it can describe a world that is not currently loaded.
 */
public record WorldPos(Key world, double x, double y, double z, float yaw, float pitch) {

    public static WorldPos of(Location location) {
        NamespacedKey worldKey = location.getWorld().getKey();
        return new WorldPos(worldKey, location.x(), location.y(), location.z(), location.getYaw(), location.getPitch());
    }

    public @Nullable World getWorld() {
        return Bukkit.getWorld(new NamespacedKey(world.namespace(), world.value()));
    }

    public @Nullable Location toLocation() {
        World bukkitWorld = getWorld();
        return bukkitWorld == null ? null : toLocation(bukkitWorld);
    }

    public Location toLocation(World bukkitWorld) {
        return new Location(bukkitWorld, x, y, z, yaw, pitch);
    }
}
