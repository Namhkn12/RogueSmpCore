package com.roguesmp.constant;

import com.google.gson.annotations.SerializedName;
import org.bukkit.attribute.Attribute;

/**
 * For use in entities only
 */
public enum EntityAttribute {

    @SerializedName("max_health")
    MAX_HEALTH(Attribute.MAX_HEALTH),
    @SerializedName("follow_range")
    FOLLOW_RANGE(Attribute.FOLLOW_RANGE),
    @SerializedName("knockback_resistance")
    KNOCKBACK_RESISTANCE(Attribute.KNOCKBACK_RESISTANCE),
    @SerializedName("movement_speed")
    MOVEMENT_SPEED(Attribute.MOVEMENT_SPEED),
    @SerializedName("flying_speed")
    FLYING_SPEED(Attribute.FLYING_SPEED),
    @SerializedName("attack_damage")
    ATTACK_DAMAGE(Attribute.ATTACK_DAMAGE),
    @SerializedName("attack_knockback")
    ATTACK_KNOCKBACK(Attribute.ATTACK_KNOCKBACK),
    @SerializedName("attack_speed")
    ATTACK_SPEED(Attribute.ATTACK_SPEED),
    @SerializedName("armor")
    ARMOR(Attribute.ARMOR),
    @SerializedName("fall_damage_multiplier")
    FALL_DAMAGE_MULTIPLIER(Attribute.FALL_DAMAGE_MULTIPLIER),
    @SerializedName("safe_fall_distance")
    SAFE_FALL_DISTANCE(Attribute.SAFE_FALL_DISTANCE),
    @SerializedName("scale")
    SCALE(Attribute.SCALE),
    @SerializedName("step_height")
    STEP_HEIGHT(Attribute.STEP_HEIGHT),
    @SerializedName("gravity")
    GRAVITY(Attribute.GRAVITY),
    @SerializedName("jump_strength")
    JUMP_STRENGTH(Attribute.JUMP_STRENGTH),
    @SerializedName("burning_time")
    BURNING_TIME(Attribute.BURNING_TIME),
    @SerializedName("explosion_knockback_resistance")
    EXPLOSION_KNOCKBACK_RESISTANCE(Attribute.EXPLOSION_KNOCKBACK_RESISTANCE),
    @SerializedName("movement_efficiency")
    MOVEMENT_EFFICIENCY(Attribute.MOVEMENT_EFFICIENCY),
    @SerializedName("water_movement_efficiency")
    WATER_MOVEMENT_EFFICIENCY(Attribute.WATER_MOVEMENT_EFFICIENCY),
    ;

    private final Attribute bukkitAttribute;

    EntityAttribute(Attribute bukkitAttribute) {
        this.bukkitAttribute = bukkitAttribute;
    }

    public Attribute getBukkitAttribute() {
        return bukkitAttribute;
    }
}
