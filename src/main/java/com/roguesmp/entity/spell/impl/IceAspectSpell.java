package com.roguesmp.entity.spell.impl;

import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellContext;
import com.roguesmp.event.DamageEvent;
import org.bukkit.entity.LivingEntity;

public class IceAspectSpell extends Spell {

    private final LivingEntity owner;

    public IceAspectSpell(LivingEntity owner) {
        this.owner = owner;
    }

    @Override
    public void onDamage(DamageEvent event) {
        event.getVictim().setFreezeTicks(40);
    }

    @Override
    public void tick(SpellContext context, int interval) {

    }
}
