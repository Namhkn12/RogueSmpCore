package com.roguesmp.registry;

import com.roguesmp.codec.Codec;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.effect.impl.DamageIncreaseEffect;
import com.roguesmp.effect.impl.ResistanceEffect;
import com.roguesmp.effect.impl.SpeedEffect;

/**
 * Codec registry for SmpEffect
 */
public class EffectCodecRegistry {

    private static void register(String effectID, Codec<? extends SmpEffect> effectCodec) {
        Registries.EFFECT_CODEC.register(effectID, effectCodec);
    }

    public static void bootstrap() {
        register(SpeedEffect.EFFECT_ID, SpeedEffect.CODEC);
        register(DamageIncreaseEffect.ID, DamageIncreaseEffect.CODEC);
        register(ResistanceEffect.ID, ResistanceEffect.CODEC);
    }
}
