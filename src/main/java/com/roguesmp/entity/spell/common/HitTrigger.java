package com.roguesmp.entity.spell.common;

import com.roguesmp.constant.DamageType;

public enum HitTrigger {
    MELEE,
    PROJECTILE,
    ANY;

    public boolean matches(DamageType type) {
        return switch (this) {
            case MELEE -> DamageType.isMeleeDamage(type);
            case PROJECTILE -> DamageType.isProjectileDamage(type);
            case ANY -> DamageType.isMeleeDamage(type) || DamageType.isProjectileDamage(type);
        };
    }
}
