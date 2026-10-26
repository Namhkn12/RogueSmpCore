package com.roguesmp.teleport;

import com.roguesmp.codec.Codec;
import com.roguesmp.teleport.effect.EndermanTeleportEffect;
import com.roguesmp.teleport.effect.NoTeleportEffect;
import com.roguesmp.teleport.effect.TeleportEffect;

public record TeleportOptions(TeleportEffect effect, int warmupTicks, boolean safe) {

    public static final int DEFAULT_WARMUP = 30;

    public static final TeleportOptions DEFAULT = new TeleportOptions(EndermanTeleportEffect.DEFAULT, DEFAULT_WARMUP, true);
    public static final TeleportOptions INSTANT = new TeleportOptions(NoTeleportEffect.INSTANCE, 0, true);
    public static final TeleportOptions EXACT = new TeleportOptions(NoTeleportEffect.INSTANCE, 0, false);

    public static final Codec<TeleportOptions> CODEC = Codec.composite(
            TeleportEffect.CODEC.optionalFieldOf("effect", EndermanTeleportEffect.DEFAULT).forGetter(TeleportOptions::effect),
            Codec.INT.optionalFieldOf("warmup", DEFAULT_WARMUP).forGetter(TeleportOptions::warmupTicks),
            Codec.BOOLEAN.optionalFieldOf("safe", true).forGetter(TeleportOptions::safe),
            TeleportOptions::new
    );

    public boolean hasEffect() {
        return !(effect instanceof NoTeleportEffect);
    }

    public TeleportOptions withEffect(TeleportEffect effect) {
        return new TeleportOptions(effect, warmupTicks, safe);
    }

    public TeleportOptions withWarmup(int warmupTicks) {
        return new TeleportOptions(effect, warmupTicks, safe);
    }

    public TeleportOptions withSafe(boolean safe) {
        return new TeleportOptions(effect, warmupTicks, safe);
    }
}
