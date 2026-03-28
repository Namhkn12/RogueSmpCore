package com.roguesmp.registry;

import com.roguesmp.effect.SmpEffect;
import com.roguesmp.effect.impl.DamageIncreaseEffect;
import com.roguesmp.effect.impl.ResistanceEffect;
import com.roguesmp.effect.impl.SpeedEffect;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple codec registry for SmpEffect
 */
public class EffectCodecRegistry {
    private static final Map<String, Class<? extends SmpEffect>> CODECS = new HashMap<>();

    public static @Nullable Class<? extends SmpEffect> get(String effectID) {
        return CODECS.get(effectID);
    }

    private static void register(String id, Class<? extends SmpEffect> clazz) {
        CODECS.put(id, clazz);
    }

    static {
        register(SpeedEffect.EFFECT_ID, SpeedEffect.class);
        register(DamageIncreaseEffect.ID, DamageIncreaseEffect.class);
        register(ResistanceEffect.ID, ResistanceEffect.class);
    }
}
