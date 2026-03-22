package com.roguesmp.dungeon.service;

import com.roguesmp.dungeon.data.Dungeon;
import java.util.List;
import java.util.Optional;

public interface IDungeonService {

    /**
     * Tạo mới một dungeon template và lưu xuống storage.
     * @return Dungeon vừa tạo
     * @throws IllegalArgumentException nếu name/description không hợp lệ
     */
    Dungeon createDungeon(String name);

    /**
     * Lấy dungeon template theo id.
     * @return Optional.empty() nếu không tìm thấy
     */
    Optional<Dungeon> getDungeonById(String dgId);

    List<String> getDungeonIdList();

    /**
     * Xóa dungeon template theo id.
     * @return true nếu xóa thành công, false nếu không tìm thấy
     */
    boolean deleteDungeon(String dgId);
}