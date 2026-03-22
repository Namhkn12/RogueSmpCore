package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.constant.DataConfig;
import com.roguesmp.dungeon.constant.PrefixConfig;
import com.roguesmp.dungeon.constant.WorldConfig;
import com.roguesmp.dungeon.data.DungeonWorld;
import com.roguesmp.dungeon.data.Region;
import com.roguesmp.dungeon.manager.RegionManager;
import com.roguesmp.dungeon.service.IRegionService;
import com.roguesmp.dungeon.utils.ConsoleLogger;
import com.roguesmp.dungeon.utils.DungeonWorldGenerator;
import com.roguesmp.dungeon.utils.Log4Craft;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.util.Optional;
import java.util.UUID;

public class RegionService implements IRegionService {

    private final RegionManager manager;

    public RegionService(RegionManager manager) {
        this.manager = manager;
        loadBukkitWorld();
    }

    @Override
    public void saveDungeonRegion() {
        manager.saveAll();
    }

    @Override
    public Optional<Region> acquireRegion() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("acquireRegion() must be called from main thread");
        }

        for (DungeonWorld world : manager.getAllWorlds()) {
            Optional<Region> region = tryAcquireFromWorld(world);
            if (region.isPresent()) return region;
        }

        if (manager.getAllWorlds().size() >= WorldConfig.MAX_WORLD_PER_SERVER) {
            Log4Craft.info("All dungeon worlds are full. Cannot create more.");
            return Optional.empty();
        }

        DungeonWorld newWorld = createNewWorld();
        if (newWorld == null) return Optional.empty();

        return tryAcquireFromWorld(newWorld);
    }

    @Override
    public void releaseRegion(Region region) {
        region.setStatus(false);
        manager.saveWorld(region.getWorldName());
    }

    private Optional<Region> tryAcquireFromWorld(DungeonWorld world) {
        Optional<Region> available = manager.findAvailableRegion(world.getWorldName());
        if (available.isPresent()) {
            available.get().setStatus(true);
            manager.saveWorld(world.getWorldName());
            return available;
        }

        int count = manager.getRegionCount(world.getWorldName());
        if (count >= WorldConfig.MAX_REGION_PER_WORLD) {
            return Optional.empty();
        }

        Region newRegion = createRegion(world.getWorldName(), count);
        manager.addRegion(world.getWorldName(), newRegion);
        newRegion.setStatus(true);
        manager.saveWorld(world.getWorldName());
        return Optional.of(newRegion);
    }

    /**
     * Tính vị trí region theo index — deterministic, không cần lưu vào file
     * Grid layout: mỗi hàng 5 region, cách nhau DISTANCE_BETWEEN_REGION block
     */
    private Region createRegion(String worldName, int index) {
        int gridWidth = WorldConfig.GRID_WIDTH;
        double x = (index % gridWidth) * WorldConfig.DISTANCE_BETWEEN_REGION;
        double z = (index / gridWidth) * WorldConfig.DISTANCE_BETWEEN_REGION;
        return new Region(UUID.randomUUID(), worldName, x, WorldConfig.REGION_Y, z);
    }

    /**
     * Tạo world dungeon mới với DungeonWorldGenerator (world trống)
     */
    private DungeonWorld createNewWorld() {
        String worldName = DataConfig.DUNGEON_TEMPLATE_FILE + UUID.randomUUID();
        WorldCreator creator = new WorldCreator(worldName);
        creator.generator(new DungeonWorldGenerator());

        World world = creator.createWorld();
        if (world == null) {
            Log4Craft.fire("Failed to create dungeon world: " + worldName, null);
            return null;
        }

        DungeonWorld dungeonWorld = new DungeonWorld(worldName);
        manager.addWorld(dungeonWorld);
        Log4Craft.success("Created new dungeon world: " + worldName);
        return dungeonWorld;
    }

    private void loadBukkitWorld() {
        for (DungeonWorld world : manager.getAllWorlds()) {
            if (Bukkit.getWorld(world.getWorldName()) == null) {
                WorldCreator creator = new WorldCreator(world.getWorldName());
                creator.generator(new DungeonWorldGenerator());
                creator.createWorld();
                Log4Craft.success("Reloaded world: " + world.getWorldName());
            }
        }

        Log4Craft.info("Loaded " + manager.getAllWorlds().size() + " dungeon worlds.");
    }
}