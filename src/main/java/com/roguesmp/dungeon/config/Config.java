package com.roguesmp.dungeon.config;

import org.bukkit.configuration.file.FileConfiguration;

public class Config {

    private final boolean enableDungeon;
    private final int maxRegionPerWorld;
    private final int maxRegionActivePerWorld;
    private final int maxWorldPerServer;
    private final int distanceBetweenRegion;
    private final int gridWidth;
    private final double regionY;

    public Config(FileConfiguration cfg) {
        // World settings — default lấy từ WorldConfig
        this.enableDungeon           = cfg.getBoolean("enable_dungeon", true);
        this.maxRegionPerWorld       = cfg.getInt("max_region_per_world",        WorldConfig.MAX_REGION_PER_WORLD);
        this.maxRegionActivePerWorld = cfg.getInt("max_region_active_per_world", WorldConfig.MAX_REGION_ACTIVE_PER_WORLD);
        this.maxWorldPerServer       = cfg.getInt("max_world_per_server",        WorldConfig.MAX_WORLD_PER_SERVER);
        this.distanceBetweenRegion   = cfg.getInt("distance_between_region",     WorldConfig.DISTANCE_BETWEEN_REGION);
        this.gridWidth               = cfg.getInt("grid_width",                  WorldConfig.GRID_WIDTH);
        this.regionY                 = cfg.getDouble("region_y",                 WorldConfig.REGION_Y);
    }

    // Getters — World
    public int getMaxRegionPerWorld()       { return maxRegionPerWorld; }
    public int getMaxRegionActivePerWorld() { return maxRegionActivePerWorld; }
    public int getMaxWorldPerServer()       { return maxWorldPerServer; }
    public int getDistanceBetweenRegion()   { return distanceBetweenRegion; }
    public int getGridWidth()               { return gridWidth; }
    public double getRegionY()              { return regionY; }
}