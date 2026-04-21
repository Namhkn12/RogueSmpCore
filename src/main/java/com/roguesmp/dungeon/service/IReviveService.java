package com.roguesmp.dungeon.service;

import com.roguesmp.dungeon.data.runtime.session.DungeonPlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface IReviveService {

    /**
     * Đăng ký player vừa chết.
     * Gọi từ handlePlayerDead() sau khi đã set checkpoint + status DEAD.
     */
    void registerPlayerDead(Player player);

    /**
     * Xử lý logic mỗi tick — được truyền vào ReviveManager.startTask().
     *
     * Luồng mỗi tick:
     *   1. Lấy snapshot tất cả DeadEntry từ manager
     *   2. Với mỗi entry, tìm rescuer hợp lệ (cùng party, trong vùng, đang shift)
     *   3a. Có rescuer  → tăng progress, cập nhật bossbar "đang cứu X%"
     *   3b. Không có    → giảm progress (decay), cập nhật bossbar "bị gián đoạn"
     *   4. Nếu progress đủ threshold → gọi revivePlayer()
     */
    void processTick();

    /**
     * Revive 1 player cụ thể.
     *
     * Luồng:
     *   1. Lấy DeadEntry từ manager
     *   2. Restore game mode, teleport về checkpoint
     *   3. Restore health/food
     *   4. Xoá entry + bossbar khỏi manager
     *   5. Gọi presenter.onPlayerRevived()
     */
    void revivePlayer(UUID deadUUID, DungeonPlayer dungeonPlayer);

    void forceRemoveDeadEntry(UUID uuid);

    /**
     * Force revive tất cả dead player — gọi khi room clear.
     *
     * Luồng:
     *   1. Lấy snapshot toàn bộ UUID từ manager
     *   2. Gọi revivePlayer() cho từng UUID
     */
    void reviveAll();

    boolean isDead(UUID uuid);
}