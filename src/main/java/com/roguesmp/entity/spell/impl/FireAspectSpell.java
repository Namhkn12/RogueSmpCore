package com.roguesmp.entity.spell.impl;

import com.roguesmp.entity.spell.Spell;
import com.roguesmp.event.DamageEvent;
import org.bukkit.entity.LivingEntity;

import java.util.Map;

public class FireAspectSpell extends Spell {


    public FireAspectSpell() {
    }

    @Override
    public void run(int interval) {
    }

    @Override
    public void onDamage(DamageEvent event) {
        event.getVictim().setFireTicks(60);
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    public static FireAspectSpell readParam(Map<String, Object> param, LivingEntity owner) {
        return new FireAspectSpell();
    }

}
