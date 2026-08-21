package com.roguesmp.attribute;

import com.roguesmp.attribute.impl.*;

public enum Attributes {
    // --- COMBAT & OFFENSE ---
    MELEE_DAMAGE_BASE(new MeleeDamageBase()),
    ATTACK_SPEED_BASE(new AttackSpeedBase()),
    PROJECTILE_DAMAGE_BASE(new ProjectileDamageBase()),
    PROJECTILE_SPEED_BASE(new ProjectileSpeedBase()),
    THROW_RATE_BASE(new ThrowRateBase()),
    MELEE_DAMAGE_PERCENT(new MeleeDamagePercent()),
    MAGIC_DAMAGE_PERCENT(new MagicDamagePercent()),
    THROW_RATE_PERCENT(new ThrowRatePercent()),
    PROJECTILE_DAMAGE_PERCENT(new ProjectileDamagePercent()),
    PROJECTILE_SPEED_PERCENT(new ProjectileSpeedPercent()),
    CRIT_DAMAGE_FLAT(new CriticalDamageFlat()),
    ATTACK_KNOCKBACK(new AttackKnockback()),
    SWEEPING_DAMAGE_RATIO(new SweepingDamageRatio()),

    // --- DEFENSE & VITALS ---
    DEFENSE_FLAT(new DefenseFlat()),
    MAX_HEALTH_FLAT(new MaxHealthFlat()),
    MAX_HEALTH_PERCENT(new MaxHealthPercent()),
    KNOCKBACK_RESISTANCE(new KnockbackResistance()),

    // --- LAND MOVEMENT ---
    SPEED_FLAT(new SpeedFlat()),
    SPEED_PERCENT(new SpeedPercent()),
    SNEAKING_SPEED(new SneakingSpeed()),
    MOVEMENT_EFFICIENCY(new MovementEfficiency()),

    // --- VERTICAL & PHYSICS ---
    JUMP_STRENGTH(new JumpStrength()),
    GRAVITY(new Gravity()),
    STEP_HEIGHT(new StepHeight()),
    FALL_DAMAGE(new FallDamage()),
    SAFE_FALL_DISTANCE(new SafeFallDistance()),

    // --- AQUATIC ---
    WATER_MOVEMENT_EFFICIENCY(new WaterMovementEfficiency()),
    SUBMERGED_MINING_SPEED(new SubmergedMiningSpeed()),
    OXYGEN_BONUS(new OxygenBonus()),

    // --- UTILITY & WORLD ---
    ENTITY_REACH(new EntityReach()),
    BLOCK_REACH(new BlockReach()),
    MINING_EFFICIENCY(new MiningEfficiency()),
    BURNING_TIME(new BurningTime()),
    SCALE(new Scale()),
    LUCK(new Luck());

    private final SmpAttribute attribute;

    Attributes(SmpAttribute attribute) {
        this.attribute = attribute;
    }

    public SmpAttribute getAttribute() {
        return attribute;
    }
}