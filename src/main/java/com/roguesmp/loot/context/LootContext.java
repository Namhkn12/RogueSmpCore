package com.roguesmp.loot.context;

import com.roguesmp.player.SmpPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mang toàn bộ thông tin runtime của một lượt roll loot.
 *
 * <p>Được tạo một lần cho mỗi sự kiện loot (mở rương, mob chết, trả thưởng quest),
 * rồi đi xuyên suốt chuỗi roll. Context mô tả <b>nơi gọi</b> ({@link #getOrigin()} +
 * {@link #getSource()}) và <b>người roll</b> ({@link #getPlayer()}).
 *
 * <p>Cách các hệ thống khác ảnh hưởng tới 1 lượt roll: đừng sửa nơi gọi roll, hãy nghe
 * {@link com.roguesmp.loot.event.LootRollEvent} (huỷ cả lượt roll),
 * {@link com.roguesmp.loot.event.LootPoolPickEvent} (sửa entry nào được chọn trong 1 pool), hoặc
 * {@link com.roguesmp.loot.event.LootEntryResultEvent} / {@link com.roguesmp.loot.event.LootRollCompleteEvent}
 * (sửa item vừa sinh ra) — mọi thứ đi thẳng vào dữ liệu thật (LootEntry/ItemStack), không qua
 * 1 lớp modifier trung gian.
 */
public class LootContext {

    private final @Nullable SmpPlayer player;
    private final @NotNull LootOrigin origin;
    private final @Nullable Object source;

    private LootContext(Builder builder) {
        this.player = builder.player;
        this.origin = builder.origin;
        this.source = builder.source;
    }

    public @Nullable SmpPlayer getPlayer() {
        return player;
    }

    /** Loại sự kiện đã gọi roll (mở rương, giết mob, ...). */
    public @NotNull LootOrigin getOrigin() {
        return origin;
    }

    /**
     * Đối tượng gốc gắn với lượt roll — {@code Block} với rương, {@code Entity} với mob,
     * null nếu caller không cung cấp.
     */
    public @Nullable Object getSource() {
        return source;
    }

    /**
     * Ép kiểu {@link #getSource()} về {@code type}, trả null nếu không khớp.
     *
     * <pre>{@code Block chest = ctx.getSourceAs(Block.class);}</pre>
     */
    public <T> @Nullable T getSourceAs(@NotNull Class<T> type) {
        return type.isInstance(source) ? type.cast(source) : null;
    }

    // --- Builder ---

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(@Nullable SmpPlayer player) {
        return new Builder().player(player);
    }

    public static class Builder {
        private @Nullable SmpPlayer player;
        private @NotNull LootOrigin origin = LootOrigin.UNKNOWN;
        private @Nullable Object source;

        public Builder player(@Nullable SmpPlayer player) {
            this.player = player;
            return this;
        }

        /** Khai báo nơi gọi roll — listener dựa vào đây để lọc. */
        public Builder origin(@NotNull LootOrigin origin, @Nullable Object source) {
            this.origin = origin;
            this.source = source;
            return this;
        }

        public Builder origin(@NotNull LootOrigin origin) {
            return origin(origin, null);
        }

        public LootContext build() {
            return new LootContext(this);
        }
    }
}
