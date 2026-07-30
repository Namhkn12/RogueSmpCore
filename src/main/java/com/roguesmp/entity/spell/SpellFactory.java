package com.roguesmp.entity.spell;

import com.roguesmp.codec.Codec;
import org.bukkit.entity.LivingEntity;

import java.util.function.BiFunction;

/**
 * Bundles a spell type's {@link SpellParams} codec together with its {@code Spell} constructor,
 * so a single {@code Registries.ENTITY_SPELL} lookup can both decode JSON and create the spell.
 * {@link #createSpell} takes the exact {@code P} type - no cast needed here, since callers that
 * already hold a matching {@code SpellFactory<P>}/{@code P} pair are trivially safe. The one place
 * that can't statically know {@code P} (looking a factory up by id at runtime) is
 * {@code EntitySpells#createSpell}, which is where the unavoidable cast lives instead.
 */
public record SpellFactory<P extends SpellParams>(Codec<P> paramsCodec, BiFunction<P, LivingEntity, Spell> factory) {

    public Spell createSpell(P params, LivingEntity owner) {
        return factory.apply(params, owner);
    }
}
