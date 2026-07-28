package com.roguesmp.entity.spell;

import com.roguesmp.codec.Codec;
import org.bukkit.entity.LivingEntity;

import java.util.function.BiFunction;

/**
 * Bundles a spell type's {@link SpellParams} codec together with its {@code Spell} constructor,
 * so a single {@code Registries.ENTITY_SPELL} lookup can both decode JSON and create the spell -
 * callers never need to know the concrete {@code Params} type.
 */
public record SpellFactory<P extends SpellParams>(Codec<P> paramsCodec, BiFunction<P, LivingEntity, Spell> factory) {

    @SuppressWarnings("unchecked")
    public Spell createSpell(SpellParams params, LivingEntity owner) {
        return factory.apply((P) params, owner);
    }
}
