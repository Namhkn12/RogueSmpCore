package com.roguesmp.entity.spell;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;

/**
 * A spell's own typed configuration, self-describing via {@link #getTypeId()} — same dispatch
 * pattern as {@code QuestReward}/{@code UpgradeRequirement}/{@code EntityComponent}. Replaces the
 * old {@code Map<String, Object>} params bag: each spell declares its own {@code Params} record
 * (see {@code SelfDestructSpell.Params} for one with real fields, or any other spell in
 * {@code entity/spell/impl} for the common zero-field case via {@link com.roguesmp.codec.MapCodec#unit}).
 */
public interface SpellParams {

    Codec<SpellParams> CODEC = Codec.dispatch(
            SpellParams::getTypeId,
            id -> Registries.ENTITY_SPELL.getOrThrow(id).paramsCodec()
    );

    String getTypeId();
}
