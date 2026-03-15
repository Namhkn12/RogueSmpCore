package com.roguesmp.dungeon.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.UUID;

public class Region {
    private UUID id;
    private String worldName;
    private double x;
    private double y;
    private double z;
    private boolean status; // false = available, true = occupied

    public Region() {}

    public Region(UUID id, String worldName, double x, double y, double z) {
        this.id = id;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.status = false;
    }

    // Location không serialize được với Gson nên dùng primitive
    public Location getRegionPoint() {
        return new Location(Bukkit.getWorld(worldName), x, y, z);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }

    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }
}