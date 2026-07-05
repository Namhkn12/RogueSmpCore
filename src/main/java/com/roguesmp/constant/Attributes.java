package com.roguesmp.constant;

import com.google.gson.annotations.SerializedName;
import com.roguesmp.attribute.SmpAttribute;
import com.roguesmp.attribute.impl.*;

public enum Attributes {
    // --- COMBAT & OFFENSE ---
    @SerializedName("melee_damage_base")
    MELEE_DAMAGE_BASE(new MeleeDamageBase()),

    @SerializedName("attack_speed_base")
    ATTACK_SPEED_BASE(new AttackSpeedBase()),

    @SerializedName("projectile_damage_base")
    PROJECTILE_DAMAGE_BASE(new ProjectileDamageBase()),

    @SerializedName("projectile_speed_base")
    PROJECTILE_SPEED_BASE(new ProjectileSpeedBase()),

    @SerializedName("throw_rate_base")
    THROW_RATE_BASE(new ThrowRateBase()),

    @SerializedName("melee_damage_percent")
    MELEE_DAMAGE_PERCENT(new MeleeDamagePercent()),

    @SerializedName("magic_damage_percent")
    MAGIC_DAMAGE_PERCENT(new MagicDamagePercent()),

    @SerializedName("throw_rate_percent")
    THROW_RATE_PERCENT(new ThrowRatePercent()),

    @SerializedName("projectile_damage_percent")
    PROJECTILE_DAMAGE_PERCENT(new ProjectileDamagePercent()),

    @SerializedName("projectile_speed_percent")
    PROJECTILE_SPEED_PERCENT(new ProjectileSpeedPercent()),

    @SerializedName("crit_damage_flat")
    CRIT_DAMAGE_FLAT(new CriticalDamageFlat()),

    @SerializedName("attack_knockback")
    ATTACK_KNOCKBACK(new AttackKnockback()),

    @SerializedName("sweeping_damage_ratio")
    SWEEPING_DAMAGE_RATIO(new SweepingDamageRatio()),

    // --- DEFENSE & VITALS ---
    @SerializedName("defense_flat")
    DEFENSE_FLAT(new DefenseFlat()),

    @SerializedName("max_health_flat")
    MAX_HEALTH_FLAT(new MaxHealthFlat()),

    @SerializedName("max_health_percent")
    MAX_HEALTH_PERCENT(new MaxHealthPercent()),

    @SerializedName("knockback_resistance")
    KNOCKBACK_RESISTANCE(new KnockbackResistance()),

    // --- LAND MOVEMENT ---
    @SerializedName("speed_flat")
    SPEED_FLAT(new SpeedFlat()),

    @SerializedName("speed_percent")
    SPEED_PERCENT(new SpeedPercent()),

    @SerializedName("sneaking_speed")
    SNEAKING_SPEED(new SneakingSpeed()),

    @SerializedName("movement_efficiency")
    MOVEMENT_EFFICIENCY(new MovementEfficiency()),

    // --- VERTICAL & PHYSICS ---
    @SerializedName("jump_strength")
    JUMP_STRENGTH(new JumpStrength()),

    @SerializedName("gravity")
    GRAVITY(new Gravity()),

    @SerializedName("step_height")
    STEP_HEIGHT(new StepHeight()),

    @SerializedName("fall_damage")
    FALL_DAMAGE(new FallDamage()),

    @SerializedName("safe_fall_distance")
    SAFE_FALL_DISTANCE(new SafeFallDistance()),

    // --- AQUATIC ---
    @SerializedName("water_movement_efficiency")
    WATER_MOVEMENT_EFFICIENCY(new WaterMovementEfficiency()),

    @SerializedName("submerged_mining_speed")
    SUBMERGED_MINING_SPEED(new SubmergedMiningSpeed()),

    @SerializedName("oxygen_bonus")
    OXYGEN_BONUS(new OxygenBonus()),
    // --- UTILITY & WORLD ---
    @SerializedName("entity_reach")
    ENTITY_REACH(new EntityReach()),

    @SerializedName("block_reach")
    BLOCK_REACH(new BlockReach()),

    @SerializedName("burning_time")
    BURNING_TIME(new BurningTime()),

    @SerializedName("scale")
    SCALE(new Scale()),

    @SerializedName("luck")
    LUCK(new Luck());

    private final SmpAttribute attribute;

    Attributes(SmpAttribute attribute) {
        this.attribute = attribute;
    }

    public SmpAttribute getAttribute() {
        return attribute;
    }
}
