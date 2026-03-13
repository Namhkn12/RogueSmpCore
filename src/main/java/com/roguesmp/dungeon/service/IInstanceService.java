package com.roguesmp.dungeon.service;

import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.instance.NodeInstance;
import com.roguesmp.dungeon.instance.RegionInstance;

import java.util.Optional;
import java.util.UUID;

public interface IInstanceService {

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    void onServerStart();
    void onServerStop();

    // -------------------------------------------------------------------------
    // Instance management
    // -------------------------------------------------------------------------

    /**
     * Tạo DungeonInstance mới, lưu vào manager và file
     */
    DungeonInstance createDungeonInstance(String dungeonId, UUID party, RegionInstance region);

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

    // -------------------------------------------------------------------------
    // Room flow
    // -------------------------------------------------------------------------

    /**
     * Roll nextRooms từ nodes còn lại sau khi player clear 1 room
     */
    void rollNextRooms(DungeonInstance instance, int nextRoomCount);

    /**
     * Player chọn room từ UI — xóa khỏi nextRooms + nodes, trả về NodeInstance được chọn
     */
    NodeInstance selectNextRoom(DungeonInstance instance, UUID nodeInstanceId);
}