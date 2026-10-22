package com.roguesmp.entity.spell;

import com.roguesmp.codec.Codec;
import org.bukkit.entity.LivingEntity;

import java.util.function.BiFunction;

public record SpellType<P extends SpellParams>(String id, Codec<P> paramsCodec, BiFunction<LivingEntity, P, Spell> factory, P defaults) {

    public SpellConfig configure(P params) {
        return new SpellConfig(this, params);
    }

    public SpellConfig defaultConfig() {
        return configure(defaults);
    }

    @SuppressWarnings("unchecked")
    Codec<SpellConfig> configCodec() {
        return paramsCodec.xmap(this::configure, config -> (P) config.params());
    }

    @SuppressWarnings("unchecked")
    Spell create(SpellParams params, LivingEntity owner) {
        return factory.apply(owner, (P) params);
    }
}
