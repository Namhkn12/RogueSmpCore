package com.roguesmp.loot.condition;

import com.roguesmp.codec.Codec;
import com.roguesmp.loot.context.LootContext;
import com.roguesmp.registry.Registries;

/**
 * Gates whether a {@link com.roguesmp.loot.LootEntry} is eligible to be picked during a
 * weighted roll — every condition attached to an entry must {@link #test(LootContext)} true
 * for it to be considered at all (a failing entry is excluded from the weight sum entirely,
 * not just zero-weighted).
 *
 * <p>New condition types register a {@code Codec} into
 * {@link com.roguesmp.loot.condition.LootConditions} to become usable from loot table JSON
 * under an entry's {@code "conditions"} array, dispatched by the {@code "type"} field —
 * same shape as {@link com.roguesmp.player.ability.upgrade.UpgradeRequirement}.
 */
public interface LootCondition {

    Codec<LootCondition> CODEC = Codec.dispatch(LootCondition::getTypeId, Registries.LOOT_CONDITION_CODEC::getOrThrow);

    String getTypeId();

    boolean test(LootContext context);
}
