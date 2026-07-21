package com.roguesmp.loot.event;

import com.roguesmp.loot.context.LootContext;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Bắn ra ngay trước khi {@link com.roguesmp.loot.service.LootService} bắt đầu roll một loot table.
 *
 * <p>Đây là điểm cắm duy nhất để mọi hệ thống (dungeon, buff, sự kiện server, item luck...)
 * ảnh hưởng tới kết quả roll mà <b>không phải sửa nơi gọi roll</b>. Listener đọc
 * {@link LootContext#getOrigin()} / {@link LootContext#getSource()} để biết roll đến từ đâu,
 * rồi gọi {@link LootContext#addModifier(String, double)} để đóng góp modifier của mình.
 *
 * <pre>{@code
 * @EventHandler
 * public void onLootRoll(LootRollEvent e) {
 *     LootContext ctx = e.getContext();
 *     if (ctx.getOrigin() != LootOrigin.CHEST) return;
 *     ctx.addModifier("my_system", 0.25);
 * }
 * }</pre>
 *
 * <p>Huỷ event ({@link #setCancelled(boolean)}) khiến lượt roll trả về danh sách rỗng.
 *
 * <p>Chỉ bắn một lần cho mỗi lệnh roll ở table gốc — các table lồng nhau
 * (entry {@code LOOT_TABLE}) dùng chung context, không bắn lại.
 */
public class LootRollEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull String lootTableId;
    private final @NotNull LootContext context;
    private boolean cancelled;

    public LootRollEvent(@NotNull String lootTableId, @NotNull LootContext context) {
        this.lootTableId = lootTableId;
        this.context = context;
    }

    /** ID của loot table gốc đang được roll, vd {@code "rogue:dungeons/dungeon_a_reward"}. */
    public @NotNull String getLootTableId() {
        return lootTableId;
    }

    /** Context của lượt roll — mutable, listener thêm modifier trực tiếp vào đây. */
    public @NotNull LootContext getContext() {
        return context;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
