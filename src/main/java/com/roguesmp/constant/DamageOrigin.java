package com.roguesmp.constant;

public enum DamageOrigin {
    ATTACK,
    ENVIRONMENT,
    ABILITY,
    SPELL,
    ENCHANT,
    EFFECT;

    public boolean isVanilla() {
        return this == ATTACK || this == ENVIRONMENT;
    }
}
