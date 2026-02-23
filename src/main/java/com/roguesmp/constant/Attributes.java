package com.roguesmp.constant;

import com.google.gson.annotations.SerializedName;
import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.attribute.impl.*;

public enum Attributes {
    @SerializedName("melee_damage_base")
    MELEE_DAMAGE_BASE(new MeleeDamageBase()),
    @SerializedName("projectile_damage_base")
    PROJECTILE_DAMAGE_BASE(new ProjectileDamageBase()),
    @SerializedName("defense_flat")
    DEFENSE_FLAT(new DefenseFlat()),
    @SerializedName("projectile_damage_percent")
    PROJECTILE_DAMAGE_PERCENT(new ProjectileDamagePercent()),
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
