package com.roguesmp.dungeon.data;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

import java.util.UUID;

public class Region {
    private UUID regionId;
    private String worldName;
    private Location regionPoint;
    private boolean status;

    public Region() {
    }

    public Region(UUID regionId, String worldName, Location regionPoint, BoundingBox regionBox, BoundingBox activeBox, boolean status) {
        this.regionId = regionId;
        this.worldName = worldName;
        this.regionPoint = regionPoint;
        this.status = status;
    }

    public UUID getRegionId() {
        return regionId;
    }

    public void setRegionId(UUID regionId) {
        this.regionId = regionId;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public Location getRegionPoint() {
        return regionPoint;
    }

    public void setRegionPoint(Location regionPoint) {
        this.regionPoint = regionPoint;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}
