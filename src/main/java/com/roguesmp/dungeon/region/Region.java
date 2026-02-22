package com.roguesmp.dungeon.region;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

import java.util.UUID;

public class Region {
    private UUID regionId;
    private String worldName;
    private Location regionPoint;
    private BoundingBox regionBox;
    private BoundingBox activeBox;
    private boolean status;

    public Region() {
    }

    public Region(UUID regionId, String worldName, Location regionPoint, BoundingBox regionBox, BoundingBox activeBox, boolean status) {
        this.regionId = regionId;
        this.worldName = worldName;
        this.regionPoint = regionPoint;
        this.regionBox = regionBox;
        this.activeBox = activeBox;
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

    public BoundingBox getRegionBox() {
        return regionBox;
    }

    public void setRegionBox(BoundingBox regionBox) {
        this.regionBox = regionBox;
    }

    public BoundingBox getActiveBox() {
        return activeBox;
    }

    public void setActiveBox(BoundingBox activeBox) {
        this.activeBox = activeBox;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}
