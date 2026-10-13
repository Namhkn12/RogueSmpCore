package com.roguesmp.dungeon.feature;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon.DungeonFeature;
import com.roguesmp.dungeon.FeatureContext;
import com.roguesmp.utils.DateUtils;

public record ExpiryFeature(DungeonExpiry duration) implements DungeonFeature<ExpiryFeature.State> {

    public static final String TYPE_KEY = "expiry";

    public static final class State {
        public static final Codec<State> CODEC = Codec.composite(
                Codec.LONG.optionalFieldOf("expires_at", 0L).forGetter(State::expiresAt),
                State::new
        );

        private long expiresAt;

        public State(long expiresAt) {
            this.expiresAt = expiresAt;
        }

        public long expiresAt() {
            return expiresAt;
        }

        public boolean isExpired(long now) {
            return expiresAt <= now;
        }
    }

    public static final Codec<ExpiryFeature> CODEC = Codec.composite(
            DungeonExpiry.CODEC.optionalFieldOf("duration", DungeonExpiry.DEFAULT).forGetter(ExpiryFeature::duration),
            ExpiryFeature::new
    );

    public static void setExpiresAt(FeatureContext<State> context, long expiresAt) {
        context.state().expiresAt = expiresAt;
        context.save();
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public State createState() {
        return new State(0);
    }

    @Override
    public Codec<State> stateCodec() {
        return State.CODEC;
    }

    @Override
    public void onStart(FeatureContext<State> context) {
        context.state().expiresAt = duration.expiresAt(DateUtils.now());
    }

    @Override
    public void onTick(FeatureContext<State> context) {
        if (context.state().isExpired(System.currentTimeMillis())) context.end("đã hết hạn");
    }
}
