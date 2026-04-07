package com.roguesmp.registry;

import com.google.gson.JsonObject;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.effect.impl.DamageIncreaseEffect;
import com.roguesmp.effect.impl.ResistanceEffect;
import com.roguesmp.effect.impl.SpeedEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Codec registry for SmpEffect
 */
public class EffectCodecRegistry {
    private static final Map<String, EffectDeserializer> CODECS = new HashMap<>();

    public static @Nullable EffectDeserializer get(String effectID) {
        return CODECS.get(effectID);
    }

    private static void register(String effectId, EffectDeserializer deserializer) {
        CODECS.put(effectId, deserializer);
    }

    @FunctionalInterface
    public interface EffectDeserializer {
        @NotNull SmpEffect deserialize(JsonObject jsonObject);
    }

    static {
        register(SpeedEffect.EFFECT_ID, SpeedEffect::deserialize);
        register(DamageIncreaseEffect.ID, DamageIncreaseEffect::deserialize);
        register(ResistanceEffect.ID, ResistanceEffect::deserialize);
    }
}
