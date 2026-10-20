package com.roguesmp.loot.context;

import com.roguesmp.player.SmpPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public @NotNull LootOrigin getOrigin() {
        return origin;
    }

    public @Nullable Object getSource() {
        return source;
    }

    public double getLuck() {
        return luck;
    }

    public double getLooting() {
        return looting;
    }

    public <T> @Nullable T getSourceAs(@NotNull Class<T> type) {
        return type.isInstance(source) ? type.cast(source) : null;
    }


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
