package com.roguesmp.entity.spell.common;

import com.roguesmp.entity.spell.Spell;
import com.roguesmp.event.DamageEvent;
import org.bukkit.entity.LivingEntity;

/**
 * Base for passive spells reacting to the owner hitting something (melee or its own projectile),
 * gated by a JSON-configurable {@link HitCondition}.
 */
public abstract class OnHitSpell extends Spell {

    protected final LivingEntity owner;
    private final HitCondition condition;

    protected OnHitSpell(LivingEntity owner, HitCondition condition) {
        this.owner = owner;
        this.condition = condition;
    }

    protected abstract void onHit(LivingEntity victim, DamageEvent event);

    @Override
    public final void onDamage(DamageEvent event) {
        if (condition.test(event)) {
            onHit((LivingEntity) event.getVictim(), event);
        }
    }
}
