package com.roguesmp.dungeon.repository;

import com.roguesmp.dungeon.data.Dungeon;

import java.util.List;

public interface IDungeonRepository {
    /**
     * Load tất cả các dungeon template file
     */
    List<Dungeon> loadAll();

    /**
     * Lưu 1 tempalte xuống file json
     */
    void save(Dungeon dungeon);

    /**
     * Xóa file dữ liệu của dungeon template theo tên file
     */
    boolean delete(String id);
}
