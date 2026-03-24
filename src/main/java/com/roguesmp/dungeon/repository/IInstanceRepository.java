package com.roguesmp.dungeon.repository;

import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.objective.ObjectiveRestoreCallback;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IInstanceRepository {

    /**
     * Lưu 1 instance xuống file json — mỗi instance 1 file
     */
    void save(DungeonInstance instance);

    /**
     * Xóa file của instance theo partyId
     */
    void delete(UUID partyId);

    /**
     * Load tất cả instance từ runtime folder — dùng khi server restart
     */
    List<DungeonInstance> loadAll(ObjectiveRestoreCallback onComplete);
}