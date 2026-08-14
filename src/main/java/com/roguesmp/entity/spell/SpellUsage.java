package com.roguesmp.entity.spell;

/**
 * Declares which spell list(s) a {@link SpellType} is meant to be placed into — the
 * {@link com.roguesmp.entity.component.impl.SpellComponent}'s {@code activeSpell}/{@code passiveSpell}
 * JSON arrays (or a hardcoded boss's active/passive {@code List<Spell>}s) don't enforce this
 * themselves, so this exists purely to tell whoever is declaring a spell what it's meant for.
 */
public enum SpellUsage {
    /**
     * Meant to be picked and cast one-at-a-time through a {@link SpellManager} rotation (has a
     * meaningful {@link Spell#cooldownTicks()}).
     */
    ACTIVE,
    /**
     * Meant to run unconditionally every passive tick interval, or to be a pure event-reaction
     * (e.g. {@link Spell#onDamage}) with a no-op {@link Spell#run(int)}.
     */
    PASSIVE,
    /**
     * Works correctly in either list.
     */
    EITHER
}
