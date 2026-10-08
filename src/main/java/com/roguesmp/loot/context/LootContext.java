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
 */
public class LootContext {

    private final @Nullable SmpPlayer player;
    private final @NotNull LootOrigin origin;
    private final @Nullable Object source;
    private final double luck;
    private final double looting;

    private LootContext(Builder builder) {
        this.player = builder.player;
        this.origin = builder.origin;
        this.source = builder.source;
        this.luck = builder.luck;
        this.looting = builder.looting;
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

    /** Phần trăm dạng thập phân scale weight của entry có {@code quality} (0.35 = +35%). */
    public double getLuck() {
        return luck;
    }

    /** Hệ số nhân thêm cho các {@code looting} function (số lượng drop). */
    public double getLooting() {
        return looting;
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
        private double luck;
        private double looting;

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

        public Builder luck(double luck) {
            this.luck = luck;
            return this;
        }

        public Builder looting(double looting) {
            this.looting = looting;
            return this;
        }

        public LootContext build() {
            return new LootContext(this);
        }
    }
}
