package com.roguesmp.entity.spell.common;

import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;

public enum HitTrigger {
    MELEE,
    PROJECTILE,
    ANY;

    public boolean matches(DamageEvent event) {
        return switch (this) {
            case MELEE -> event.isScalable(DamageType.MELEE);
            case PROJECTILE -> event.isScalable(DamageType.PROJECTILE);
            case ANY -> event.isScalable(DamageType.MELEE) || event.isScalable(DamageType.PROJECTILE);
        };
    }
}
