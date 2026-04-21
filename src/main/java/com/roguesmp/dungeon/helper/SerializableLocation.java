package com.roguesmp.dungeon.helper;

import org.bukkit.Bukkit;
import org.bukkit.Location;

/**
 * Gson-safe representation of a Bukkit Location.
 * Lưu worldName thay vì World object.
 */
public class SerializableLocation {

    private String world;
    private double x, y, z;
    private float yaw, pitch;

    public SerializableLocation() {}

    public SerializableLocation(String world, double x, double y, double z, float yaw, float pitch) {
        this.world = world;
        this.x = x; this.y = y; this.z = z;
        this.yaw = yaw; this.pitch = pitch;
    }

    public static SerializableLocation from(Location loc) {
        return new SerializableLocation(
                loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch()
        );
    }

    /**
     * Convert ngược lại — có thể trả null nếu world chưa load.
     */
    public Location toBukkit() {
        if (world == null) return null;
        return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }

    public String getWorld() { return world; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
}