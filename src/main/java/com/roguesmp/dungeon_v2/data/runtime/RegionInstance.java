package com.roguesmp.dungeon_v2.data.runtime;

import org.bukkit.Bukkit;
import org.bukkit.Location;

/**
 * Snapshot of the exact world position used by one dungeon run.
 */
public class RegionInstance {
    private String id;
    private String worldName;
    private double x;
    private double y;
    private double z;

    public RegionInstance() {
    }

    public RegionInstance(String id, String worldName, double x, double y, double z) {
        this.id = id;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public RegionInstance(String id, Location location) {
        this.id = id;
        setLocation(location);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public Location getLocation() {
        return new Location(Bukkit.getWorld(worldName), x, y, z);
    }

    public void setLocation(Location location) {
        if (location == null) {
            return;
        }

        this.worldName = location.getWorld() != null ? location.getWorld().getName() : null;
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
    }
}
