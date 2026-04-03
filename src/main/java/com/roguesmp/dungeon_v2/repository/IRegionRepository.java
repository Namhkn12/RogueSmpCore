package com.roguesmp.dungeon_v2.repository;

import com.roguesmp.dungeon_v2.data.definition.DungeonWorld;
import java.util.List;
import java.util.Optional;

public interface IRegionRepository {

    /**
     * Load tất cả DungeonWorld từ file json trong region folder
     */
    List<DungeonWorld> loadAll();

    /**
     * Lưu 1 DungeonWorld vào file json tương ứng
     */
    void save(DungeonWorld dungeonWorld);

    /**
     * Load 1 DungeonWorld theo worldName
     */
    Optional<DungeonWorld> loadByWorldName(String worldName);

    /**
     * Xóa file json của 1 world (dùng khi cron job dọn world trống)
     */
    void delete(String worldName);
}