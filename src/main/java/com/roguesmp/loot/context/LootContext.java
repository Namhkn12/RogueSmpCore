package com.roguesmp.loot.context;

import com.roguesmp.player.SmpPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mang toàn bộ thông tin runtime của một lượt roll loot.
 *
 * <p>Được tạo một lần cho mỗi sự kiện loot (mở rương, mob chết, trả thưởng quest),
 * rồi đi xuyên suốt chuỗi roll. Context mô tả <b>nơi gọi</b> ({@link #getOrigin()} +
 * {@link #getSource()}), <b>người roll</b> ({@link #getPlayer()}) và tập
 * <b>modifier</b> ảnh hưởng tới số lần roll thưởng.
 *
 * <p>Modifier được lưu theo dạng {@code source → value} nên mỗi hệ thống chỉ ghi đè
 * phần đóng góp của chính nó, và có thể debug được ai cộng bao nhiêu.
 *
 * <p>Cách các hệ thống khác cắm vào: đừng sửa nơi gọi roll, hãy nghe
 * {@link com.roguesmp.loot.event.LootRollEvent} rồi gọi {@link #addModifier(String, double)}:
 * <pre>{@code
 * @EventHandler
 * public void onLootRoll(LootRollEvent e) {
 *     if (e.getContext().getOrigin() != LootOrigin.CHEST) return;
 *     e.getContext().addModifier("luck_potion", 0.5);
 * }
 * }</pre>
 */
public class LootContext {

    private final @Nullable SmpPlayer player;
    private final @NotNull LootOrigin origin;
    private final @Nullable Object source;

    /** source → modifier. LinkedHashMap để giữ thứ tự cộng, tiện log/debug. */
    private final Map<String, Double> modifiers = new LinkedHashMap<>();

    private LootContext(Builder builder) {
        this.player = builder.player;
        this.origin = builder.origin;
        this.source = builder.source;
        this.modifiers.putAll(builder.modifiers);
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

    /**
     * Đăng ký (hoặc ghi đè) phần đóng góp modifier của một nguồn.
     *
     * @param source khoá định danh nguồn, vd {@code "dungeon_score"}, {@code "double_chest"}
     * @param value  giá trị cộng vào tổng modifier. Cho phép số âm (penalty).
     */
    public void addModifier(@NotNull String source, double value) {
        modifiers.put(source, value);
    }

    /**
     * Cộng dồn vào modifier sẵn có của {@code source} thay vì ghi đè.
     * Dùng khi nhiều hiệu ứng cùng loại có thể stack.
     */
    public void stackModifier(@NotNull String source, double value) {
        modifiers.merge(source, value, Double::sum);
    }

    public void removeModifier(@NotNull String source) {
        modifiers.remove(source);
    }

    public @NotNull @Unmodifiable Map<String, Double> getModifiers() {
        return Collections.unmodifiableMap(modifiers);
    }

    /**
     * Tổng modifier của mọi nguồn đã đăng ký.
     *
     * <p>Engine tính: {@code floor(pool.bonusRolls * modifier)} lần roll thêm.
     *
     * <p>Ví dụ: pool có bonus_rolls=1.0, hai nguồn mỗi nguồn 0.5 → modifier=1.0 → +1 roll.
     */
    public double getBonusRollModifier() {
        double sum = 0.0;
        for (double value : modifiers.values()) {
            sum += value;
        }
        return sum;
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
        private final Map<String, Double> modifiers = new LinkedHashMap<>();

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

        public Builder addModifier(@NotNull String source, double value) {
            this.modifiers.put(source, value);
            return this;
        }

        public LootContext build() {
            return new LootContext(this);
        }
    }
}
