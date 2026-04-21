package com.roguesmp.entity.spell.impl;

import com.roguesmp.entity.spell.Spell;
import com.roguesmp.event.DamageEvent;
import java.util.Map;

public class FireAspectSpell extends Spell {


    public FireAspectSpell() {
    }

    @Override
    public void run() {
    }

    @Override
    public void onHurt(DamageEvent event) {
        event.getVictim().setFireTicks(60);
    }

    @Override
    public int cooldownTicks() {
        return 7;
    }

    public static FireAspectSpell readParam(Map<String, Object> param) {
        return new FireAspectSpell();
    }

}
