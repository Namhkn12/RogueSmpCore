package com.roguesmp.loot.context;

/**
 * Phân loại nơi phát sinh một lượt roll loot.
 *
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

    /** Câu cá. Source là {@link org.bukkit.entity.FishHook}. */
    FISHING,

    /** Gọi thủ công từ command (debug/test). */
    COMMAND,

    /** Không xác định — mặc định khi caller không khai báo. */
    UNKNOWN
}
