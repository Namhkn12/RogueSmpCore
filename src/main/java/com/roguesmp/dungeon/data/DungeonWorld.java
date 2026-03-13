package com.roguesmp.dungeon.data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class DungeonWorld {
    private String worldName;
    private Map<UUID, Region> regions;

    public DungeonWorld() {
        this.regions = new LinkedHashMap<>();
    }

    public DungeonWorld(String worldName) {
        this.worldName = worldName;
        this.regions = new LinkedHashMap<>();
    }

    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }

    public Map<UUID, Region> getRegions() { return regions; }
    public void setRegions(Map<UUID, Region> regions) { this.regions = regions; }
}