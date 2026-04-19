package com.roguesmp.dungeon_v2.data.definition;

import com.roguesmp.dungeon_v2.data.runtime.Region;
import java.util.LinkedHashMap;
import java.util.Map;

public class DungeonWorld {
    private String worldName;
    private Map<String, Region> regions;

    public DungeonWorld() {
        this.regions = new LinkedHashMap<>();
    }

    public DungeonWorld(String worldName) {
        this.worldName = worldName;
        this.regions = new LinkedHashMap<>();
    }

    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }

    public Map<String, Region> getRegions() { return regions; }
    public void setRegions(Map<String, Region> regions) { this.regions = regions != null ? regions : new LinkedHashMap<>(); }
}
