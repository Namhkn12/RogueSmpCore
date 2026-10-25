package com.roguesmp.entity.spell.impl;

import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellContext;
import com.roguesmp.event.DamageEvent;
import org.bukkit.entity.LivingEntity;

public class FireAspectSpell extends Spell {

    public FireAspectSpell() {
    }

    @Override
    public void tick(SpellContext context, int interval) {
    }

    @Override
    public void onDamage(DamageEvent event) {
        event.getVictim().setFireTicks(60);
    }
}
