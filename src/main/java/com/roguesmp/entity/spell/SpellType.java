package com.roguesmp.entity.spell;

import com.roguesmp.codec.Codec;
import org.bukkit.entity.LivingEntity;

import java.util.function.BiFunction;

/**
 * Bundles a spell type's {@link SpellParams} codec together with its {@code Spell} constructor and
 * its {@link SpellUsage}, so a single {@code Registries.ENTITY_SPELL} lookup can decode JSON,
 * create the spell, and tell whoever is declaring it whether it belongs in an active or passive
 * spell list. {@link #createSpell} takes the exact {@code P} type - no cast needed here, since
 * callers that already hold a matching {@code SpellType<P>}/{@code P} pair are trivially safe. The
 * one place that can't statically know {@code P} (looking a type up by id at runtime) is
 * {@code EntitySpells#createSpell}, which is where the unavoidable cast lives instead.
 */
public record SpellType<P extends SpellParams>(Codec<P> paramsCodec, BiFunction<P, LivingEntity, Spell> factory, SpellUsage usage) {

    public Spell createSpell(P params, LivingEntity owner) {
        return factory.apply(params, owner);
    }
}
