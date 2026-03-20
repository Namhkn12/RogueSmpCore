package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.constraint.FolderConfig;
import com.roguesmp.dungeon.constraint.PrefixConfig;
import com.roguesmp.dungeon.constraint.WorldConfig;
import com.roguesmp.dungeon.data.DungeonWorld;
import com.roguesmp.dungeon.data.Region;
import com.roguesmp.dungeon.manager.RegionManager;
import com.roguesmp.dungeon.service.IRegionService;
import com.roguesmp.dungeon.ultis.ConsoleLogger;
import com.roguesmp.dungeon.ultis.DungeonWorldGenerator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class RegionService implements IRegionService {

    private final RegionManager manager;

    public RegionService(RegionManager manager) {
        this.manager = manager;
        loadBukkitWorld();
    }

    @Override
    public void saveDungeonRegion() {
        manager.saveAll();
        ConsoleLogger.info(PrefixConfig.REGION, "Saved all dungeon worlds.");
    }

    @Override
    public Optional<Region> acquireRegion() {
        // Phải chạy trên main thread để tránh race condition
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("acquireRegion() must be called from main thread");
        }

        // Duyệt từng world, tìm world còn slot
        for (DungeonWorld world : manager.getAllWorlds()) {
            Optional<Region> region = tryAcquireFromWorld(world);
            if (region.isPresent()) return region;
        }

        // Tất cả world đã đầy → thử tạo world mới
        if (manager.getAllWorlds().size() >= WorldConfig.MAX_WORLD_PER_SERVER) {
            ConsoleLogger.info(PrefixConfig.REGION, "All dungeon worlds are full. Cannot create more.");
            return Optional.empty();
        }

        DungeonWorld newWorld = createNewWorld();
        if (newWorld == null) return Optional.empty();

        return tryAcquireFromWorld(newWorld);
    }

    @Override
    public void releaseRegion(Region region) {
        region.setStatus(false); // trả về available
        manager.saveWorld(region.getWorldName());
    }

    /**
     * Thử lấy region available từ world, hoặc tạo thêm region mới nếu còn slot
     */
    private Optional<Region> tryAcquireFromWorld(DungeonWorld world) {
        Optional<Region> available = manager.findAvailableRegion(world.getWorldName());
        if (available.isPresent()) {
            available.get().setStatus(true);
            manager.saveWorld(world.getWorldName());
            return available;
        }

        // Không có available → thử tạo region mới nếu chưa đầy slot
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
        String worldName = FolderConfig.DUNGEON_WORLD_FILE + UUID.randomUUID();
        WorldCreator creator = new WorldCreator(worldName);
        creator.generator(new DungeonWorldGenerator());

        World world = creator.createWorld();
        if (world == null) {
            ConsoleLogger.info(PrefixConfig.REGION,"Failed to create dungeon world: " + worldName);
            return null;
        }

        DungeonWorld dungeonWorld = new DungeonWorld(worldName);
        manager.addWorld(dungeonWorld);
        ConsoleLogger.info(PrefixConfig.REGION, "Created new dungeon world: " + worldName);
        return dungeonWorld;
    }

    private void loadBukkitWorld() {
        // Load lại tất cả world dungeon vào Bukkit
        for (DungeonWorld world : manager.getAllWorlds()) {
            if (Bukkit.getWorld(world.getWorldName()) == null) {
                WorldCreator creator = new WorldCreator(world.getWorldName());
                creator.generator(new DungeonWorldGenerator());
                creator.createWorld();
                ConsoleLogger.info(PrefixConfig.REGION,"Reloaded world: " + world.getWorldName());
            }
        }

        ConsoleLogger.info(PrefixConfig.REGION,"Loaded " + manager.getAllWorlds().size() + " dungeon worlds.");
    }
}