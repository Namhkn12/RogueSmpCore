package com.roguesmp.dungeon.region;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.*;
import java.util.stream.Collectors;

public class RegionManager {

    // Key theo regionId cho đúng với Region
    private final Map<UUID, Region> regions = new HashMap<>();

    /* ==============================
       CRUD CƠ BẢN
       ============================== */

    public void addRegion(Region region) {
        if (region == null || region.getRegionId() == null) return;
        regions.put(region.getRegionId(), region);
    }

    public void removeRegion(UUID regionId) {
        regions.remove(regionId);
    }

    public Region getRegion(UUID regionId) {
        return regions.get(regionId);
    }

    public Collection<Region> getAllRegions() {
        return regions.values();
    }

    public void clear() {
        regions.clear();
    }

    /* ==============================
       STATUS CONTROL
       ============================== */

    public void setRegionStatus(UUID regionId, boolean status) {
        Region region = regions.get(regionId);
        if (region != null) {
            region.setStatus(status);
        }
    }

    public boolean isRegionActive(UUID regionId) {
        Region region = regions.get(regionId);
        return region != null && region.isStatus();
    }

    /* ==============================
       WORLD FILTER
       ============================== */

    public List<Region> getRegionsByWorld(String worldName) {
        return regions.values().stream()
                .filter(r -> r.getWorldName().equalsIgnoreCase(worldName))
                .collect(Collectors.toList());
    }

    /* ==============================
       LOCATION CHECK
       ============================== */

    public Region getRegionAt(Location location) {
        if (location == null || location.getWorld() == null) return null;

        for (Region region : regions.values()) {
            if (!region.isStatus()) continue;
            if (!region.getWorldName().equalsIgnoreCase(location.getWorld().getName())) continue;

            if (region.getRegionBox() != null &&
                    region.getRegionBox().contains(location.toVector())) {
                return region;
            }
        }
        return null;
    }

    public boolean isInsideRegion(Location location) {
        return getRegionAt(location) != null;
    }

    public Region getActiveRegionAt(Location location) {
        if (location == null || location.getWorld() == null) return null;

        for (Region region : regions.values()) {
            if (!region.isStatus()) continue;
            if (!region.getWorldName().equalsIgnoreCase(location.getWorld().getName())) continue;

            if (region.getActiveBox() != null &&
                    region.getActiveBox().contains(location.toVector())) {
                return region;
            }
        }
        return null;
    }

    public boolean isInsideActiveRegion(Location location) {
        return getActiveRegionAt(location) != null;
    }
}