package com.roguesmp.dungeon.service.impl;

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

    private static final double REGION_Y = 64.0;
    // prefix để nhận biết world dungeon khi scan
    private static final String WORLD_PREFIX = "dungeon_";

    private final RegionManager manager;

    public RegionService(RegionManager manager) {
        this.manager = manager;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void saveDungeonRegion() {
        manager.saveAll();
        ConsoleLogger.info("[RegionService] Saved all dungeon worlds.");
    }

    // -------------------------------------------------------------------------
    // Core logic
    // -------------------------------------------------------------------------

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
            ConsoleLogger.info("[RegionService] All dungeon worlds are full. Cannot create more.");
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

    @Override
    public void onServerStart() {
        manager.loadAll();

        // Load lại tất cả world dungeon vào Bukkit
        for (DungeonWorld world : manager.getAllWorlds()) {
            if (Bukkit.getWorld(world.getWorldName()) == null) {
                WorldCreator creator = new WorldCreator(world.getWorldName());
                creator.generator(new DungeonWorldGenerator());
                creator.createWorld();
                ConsoleLogger.info("[RegionService] Reloaded world: " + world.getWorldName());
            }
        }

        ConsoleLogger.info("[RegionService] Loaded " + manager.getAllWorlds().size() + " dungeon worlds.");
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Thử lấy region available từ world, hoặc tạo thêm region mới nếu còn slot
     */
    private Optional<Region> tryAcquireFromWorld(DungeonWorld world) {
        // Tìm region available trước
        Optional<Region> available = manager.findAvailableRegion(world.getWorldName());
        if (available.isPresent()) {
            available.get().setStatus(true); // đánh dấu occupied
            manager.saveWorld(world.getWorldName());
            return available;
        }

        // Không có available → thử tạo region mới nếu chưa đầy slot
        int count = manager.getRegionCount(world.getWorldName());
        if (count >= WorldConfig.MAX_REGION_PER_WORLD) {
            return Optional.empty(); // world này đầy
        }

        Region newRegion = createRegion(world.getWorldName(), count);
        manager.addRegion(world.getWorldName(), newRegion);
        newRegion.setStatus(true); // occupied ngay
        manager.saveWorld(world.getWorldName());

        ConsoleLogger.info("[RegionService] Created new region #" + count
                + " in world " + world.getWorldName());

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

        return new Region(UUID.randomUUID(), worldName, x, REGION_Y, z);
    }

    /**
     * Tạo world dungeon mới với DungeonWorldGenerator (world trống)
     */
    private DungeonWorld createNewWorld() {
        String worldName = WORLD_PREFIX + UUID.randomUUID();

        WorldCreator creator = new WorldCreator(worldName);
        creator.generator(new DungeonWorldGenerator());

        World world = creator.createWorld();
        if (world == null) {
            ConsoleLogger.info("[RegionService] Failed to create dungeon world: " + worldName);
            return null;
        }

        DungeonWorld dungeonWorld = new DungeonWorld(worldName);
        manager.addWorld(dungeonWorld); // addWorld ghi file luôn
        ConsoleLogger.info("[RegionService] Created new dungeon world: " + worldName);

        return dungeonWorld;
    }
}