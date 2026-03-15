package com.roguesmp.dungeon.service;

import com.roguesmp.dungeon.data.Dungeon;
import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.instance.NodeInstance;
import com.roguesmp.dungeon.instance.RegionInstance;

import java.util.Optional;
import java.util.UUID;

public interface IInstanceService {

    void onServerStop();

    /**
     * Tạo DungeonInstance mới, lưu vào manager và file
     */
    DungeonInstance createDungeonInstance(Dungeon dungeon, UUID party, RegionInstance region);

    /**
     * Kết thúc dungeon — xóa instance khỏi memory và file
     */
    void endDungeonInstance(UUID partyId);

    /**
     * Lưu state hiện tại của instance (sau checkpoint, clear room, ...)
     */
    void saveInstance(UUID partyId);

    /**
     * Lấy instance đang active của party
     */
    Optional<DungeonInstance> getInstance(UUID partyId);

    boolean hasActiveInstance(UUID partyId);

    /**
     * Roll nextRooms từ nodes còn lại sau khi player clear 1 room
     */
    void rollNextRooms(DungeonInstance instance);

    /**
     * Player chọn room từ UI — xóa khỏi nextRooms + nodes, trả về NodeInstance được chọn
     */
    NodeInstance selectNextRoom(DungeonInstance instance, UUID nodeInstanceId);
}