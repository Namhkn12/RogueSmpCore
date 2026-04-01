package com.roguesmp.dungeon_v2.data.runtime;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.UUID;

/**
 * Persistent region slot that can host a dungeon instance.
 */
public class Region {
    private UUID id;
    private String worldName;
    private double x;
    private double y;
    private double z;
    private RegionStatus status;

    public Region() {
        this.status = RegionStatus.AVAILABLE;
    }

    public Region(UUID id, String worldName, double x, double y, double z) {
        this.id = id;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.status = RegionStatus.AVAILABLE;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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

    public RegionStatus getStatus() {
        return status;
    }

    public void setStatus(RegionStatus status) {
        this.status = status;
    }

    public Location getRegionPoint() {
        return new Location(Bukkit.getWorld(worldName), x, y, z);
    }

    public void setRegionPoint(Location location) {
        if (location == null) {
            return;
        }

        this.worldName = location.getWorld() != null ? location.getWorld().getName() : null;
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
    }
}
