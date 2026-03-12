package com.roguesmp.dungeon.manager;

import com.roguesmp.dungeon.data.Region;
import org.bukkit.Location;

import java.util.*;
import java.util.stream.Collectors;

public class RegionManager {

    private final Map<UUID, Region> regions = new HashMap<>();

    public void createRegion(Region region) {
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

    public List<Region> getRegionsByWorld(String worldName) {
        return regions.values().stream()
                .filter(r -> r.getWorldName().equalsIgnoreCase(worldName))
                .collect(Collectors.toList());
    }
}