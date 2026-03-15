package com.roguesmp.dungeon.manager;

import com.roguesmp.dungeon.data.DungeonWorld;
import com.roguesmp.dungeon.data.Region;
import com.roguesmp.dungeon.repository.IRegionRepository;

import java.util.*;

/**
 * Quản lý state in-memory của tất cả DungeonWorld và Region.
 */
public class RegionManager {

    private final IRegionRepository repository;

    // key = worldName
    private final Map<String, DungeonWorld> worldMap = new LinkedHashMap<>();

    // flat index để lookup region theo id mà không cần duyệt worldMap
    private final Map<UUID, Region> regionIndex = new HashMap<>();

    public RegionManager(IRegionRepository repository) {
        this.repository = repository;

        loadAll();
    }

    /**
     * Gọi khi server start — load toàn bộ từ file rồi build index
     */
    public void loadAll() {
        worldMap.clear();
        regionIndex.clear();

        List<DungeonWorld> worlds = repository.loadAll();
        for (DungeonWorld world : worlds) {
            worldMap.put(world.getWorldName(), world);
            world.getRegions().forEach((id, region) -> regionIndex.put(id, region));
        }
    }

    /**
     * Gọi khi server close — ghi toàn bộ xuống file
     */
    public void saveAll() {
        worldMap.values().forEach(repository::save);
    }

    /**
     * Ghi 1 world cụ thể (dùng sau khi thêm region mới)
     */
    public void saveWorld(String worldName) {
        DungeonWorld world = worldMap.get(worldName);
        if (world != null) {
            repository.save(world);
        }
    }

    public void addWorld(DungeonWorld world) {
        worldMap.put(world.getWorldName(), world);
        repository.save(world);
    }

    public Optional<DungeonWorld> getWorld(String worldName) {
        return Optional.ofNullable(worldMap.get(worldName));
    }

    public Collection<DungeonWorld> getAllWorlds() {
        return Collections.unmodifiableCollection(worldMap.values());
    }

    public void addRegion(String worldName, Region region) {
        DungeonWorld world = worldMap.get(worldName);
        if (world == null) return;
        world.getRegions().put(region.getId(), region);
        regionIndex.put(region.getId(), region);
    }

    public Optional<Region> getRegionById(UUID regionId) {
        return Optional.ofNullable(regionIndex.get(regionId));
    }

    /**
     * Tìm region available đầu tiên trong 1 world
     */
    public Optional<Region> findAvailableRegion(String worldName) {
        DungeonWorld world = worldMap.get(worldName);
        if (world == null) return Optional.empty();

        return world.getRegions().values()
                .stream()
                .filter(r -> !r.isStatus()) // false = available
                .findFirst();
    }

    public int getRegionCount(String worldName) {
        DungeonWorld world = worldMap.get(worldName);
        return world == null ? 0 : world.getRegions().size();
    }
}