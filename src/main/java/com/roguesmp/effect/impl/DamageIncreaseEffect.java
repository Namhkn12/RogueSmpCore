package com.roguesmp.effect.impl;

import com.roguesmp.constant.DamageOperation;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.event.DamageEvent;

public class DamageIncreaseEffect extends SmpEffect {

    public static final String ID = "damage_increase";

    private final double increaseValue;

    public DamageIncreaseEffect(int duration, double increaseValue) {
        super(duration, ID);
        this.increaseValue = increaseValue;
    }

    @Override
    public double getMagnitude() {
        return increaseValue;
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public void onDamage(DamageEvent event) {
        event.addDamageModifier(increaseValue, DamageOperation.ADD_FINAL);
    }
}
