package com.roguesmp.loot.context;

/**
 * Phân loại nơi phát sinh một lượt roll loot.
 *
 * <p>Listener của {@link com.roguesmp.loot.event.LootRollEvent} dùng giá trị này
 * để lọc nhanh trước khi kiểm tra điều kiện nặng hơn.
 */
public enum LootOrigin {

    /** Mở rương (reward chest, treasure chest...). Source thường là {@link org.bukkit.block.Block}. */
    CHEST,

    /** Giết mob. Source thường là {@link org.bukkit.entity.Entity}. */
    ENTITY,

    /** Phá block. Source thường là {@link org.bukkit.block.Block}. */
    BLOCK,

    /** Trả thưởng quest. */
    QUEST,

    /** Gọi thủ công từ command (debug/test). */
    COMMAND,

    /** Không xác định — mặc định khi caller không khai báo. */
    UNKNOWN
}
