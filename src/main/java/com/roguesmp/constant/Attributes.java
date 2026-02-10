package com.roguesmp.constant;

import com.google.gson.annotations.SerializedName;
import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.attribute.impl.AttackSpeedBase;
import com.roguesmp.attribute.impl.CriticalDamageFlat;
import com.roguesmp.attribute.impl.PhysicalDamageBase;
import com.roguesmp.attribute.impl.SpeedFlat;

public enum Attributes {
    @SerializedName("physical_damage_base")
    PHYSICAL_DAMAGE_BASE(new PhysicalDamageBase()),
    @SerializedName("attack_speed_base")
    ATTACK_SPEED_BASE(new AttackSpeedBase()),
    @SerializedName("crit_damage_flat")
    CRIT_DAMAGE_FLAT(new CriticalDamageFlat()),
    @SerializedName("speed_flat")
    SPEED_FLAT(new SpeedFlat())
    ;

    private final SmpAttribute attribute;

    Attributes(SmpAttribute attribute) {
        this.attribute = attribute;
    }

    public SmpAttribute getAttribute() {
        return attribute;
    }
}
