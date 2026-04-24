package com.roguesmp.entity.spell.impl;

import com.roguesmp.entity.spell.Spell;
import com.roguesmp.event.DamageEvent;
import org.bukkit.entity.LivingEntity;

import java.util.Map;

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
    public void run(int interval) {

    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    public static IceAspectSpell readParam(Map<String, Object> param, LivingEntity owner) {
        return new IceAspectSpell(owner);
    }

}
