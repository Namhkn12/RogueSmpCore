package com.roguesmp.constant;

import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.attribute.impl.AttackSpeed;
import com.roguesmp.attribute.impl.CriticalDamage;
import com.roguesmp.attribute.impl.PhysicalAttackDamage;
import com.roguesmp.attribute.impl.Speed;

public enum Attributes {
    PHYSICAL_ATTACK_DAMAGE(new PhysicalAttackDamage()),
    CRITICAL_DAMAGE(new CriticalDamage()),
    ATTACK_SPEED(new AttackSpeed()),
    SPEED(new Speed())
    ;

    private final SmpAttribute attribute;

    Attributes(SmpAttribute attribute) {
        this.attribute = attribute;
    }

    public SmpAttribute getAttribute() {
        return attribute;
    }
}
