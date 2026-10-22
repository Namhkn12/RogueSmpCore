package com.roguesmp.entity.spell;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;
import org.bukkit.entity.LivingEntity;

public record SpellConfig(SpellType<?> type, SpellParams params) {

    public static final Codec<SpellConfig> CODEC = Codec.dispatch(
            config -> config.type().id(),
            id -> Registries.ENTITY_SPELL.getOrThrow(id).configCodec()
    );

    public Spell create(LivingEntity owner) {
        return type.create(params, owner);
    }
}
