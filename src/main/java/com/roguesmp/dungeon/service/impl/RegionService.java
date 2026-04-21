package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.utils.DungeonWorldGenerator;
import com.roguesmp.dungeon.config.DataFolderConfig;
import com.roguesmp.dungeon.config.WorldConfig;
import com.roguesmp.dungeon.data.definition.DungeonWorld;
import com.roguesmp.dungeon.data.runtime.Region;
import com.roguesmp.dungeon.data.runtime.RegionStatus;
import com.roguesmp.dungeon.manager.RegionManager;
import com.roguesmp.dungeon.service.IRegionService;
import com.roguesmp.dungeon.utils.Log4Craft_;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

public class RegionService implements IRegionService {

    private final RegionManager manager;
    private final Log4Craft_ logger;

    public RegionService(RegionManager manager, Log4Craft_ logger) {
        this.manager = manager;
        this.logger = logger;
        loadBukkitWorld();
    }

    @Override
    public void saveDungeonRegion() {
        manager.saveAll();
    }

    @Override
    public Region acquireRegion() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("acquireRegion() must be called from main thread");
        }

        for (DungeonWorld world : manager.getAllWorlds()) {
            Region region = tryAcquireFromWorld(world);
            if (region != null) return region;
        }

        if (manager.getAllWorlds().size() >= WorldConfig.MAX_WORLD_PER_SERVER) {
            logger.info(this.getClass(),"All dungeon worlds are full. Cannot create more.");
            return null;
        }

        DungeonWorld newWorld = createNewWorld();
        if (newWorld == null) return null;

        return tryAcquireFromWorld(newWorld);
    }

    @Override
    public Region getRegionById(String reid) {
        return manager.getRegionById(reid);
    }

    @Override
    public void releaseRegion(Region region) {
        region.setStatus(RegionStatus.AVAILABLE);
        manager.saveWorld(region.getWorldName());
    }

    private Region tryAcquireFromWorld(DungeonWorld world) {
        Region available = manager.findAvailableRegion(world.getWorldName());
        if (available != null) {
            available.setStatus(RegionStatus.OCCUPIED);
            manager.saveWorld(world.getWorldName());
            return available;
        }

        int count = manager.getRegionCount(world.getWorldName());
        if (count >= WorldConfig.MAX_REGION_PER_WORLD) {
            return null;
        }

        Region newRegion = createRegion(world.getWorldName(), count);
        manager.addRegion(world.getWorldName(), newRegion);
        newRegion.setStatus(RegionStatus.OCCUPIED);
        manager.saveWorld(world.getWorldName());
        return newRegion;
    }

    /**
     * Tính vị trí region theo index — deterministic, không cần lưu vào file
     * Grid layout: mỗi hàng 5 region, cách nhau DISTANCE_BETWEEN_REGION block
     */
    private Region createRegion(String worldName, int index) {
        int gridWidth = WorldConfig.GRID_WIDTH;
        double x = (index % gridWidth) * WorldConfig.DISTANCE_BETWEEN_REGION;
        double z = ((double) index / gridWidth) * WorldConfig.DISTANCE_BETWEEN_REGION;
        return new Region(java.util.UUID.randomUUID().toString(), worldName, x, WorldConfig.REGION_Y, z);
    }

    /**
     * Tạo world dungeon mới với DungeonWorldGenerator (world trống)
     */
    private DungeonWorld createNewWorld() {
        String worldName = String.join("_", DataFolderConfig.DUNGEON_TEMPLATE_FILE, java.util.UUID.randomUUID().toString());
        WorldCreator creator = new WorldCreator(worldName);
        creator.generator(new DungeonWorldGenerator());

        World world = creator.createWorld();
        if (world == null) {
            logger.fire(this.getClass(),"Failed to create dungeon world: " + worldName, null);
            return null;
        }

        DungeonWorld dungeonWorld = new DungeonWorld(worldName);
        manager.addWorld(dungeonWorld);
        logger.info(this.getClass(),"Created new dungeon world: " + worldName);
        return dungeonWorld;
    }

    private void loadBukkitWorld() {
        for (DungeonWorld world : manager.getAllWorlds()) {
            if (Bukkit.getWorld(world.getWorldName()) == null) {
                WorldCreator creator = new WorldCreator(world.getWorldName());
                creator.generator(new DungeonWorldGenerator());
                creator.createWorld();
                logger.info(this.getClass(), "Reloaded world: " + world.getWorldName());
            }
        }

        logger.info(this.getClass(),"Loaded " + manager.getAllWorlds().size() + " dungeon worlds.");
    }
}
