package com.roguesmp.constant;

import com.roguesmp.RogueSmpCore;
import org.bukkit.event.entity.EntityDamageEvent;

public enum DamageType {
    MELEE,
    MELEE_ABILITY,
    PROJECTILE,
    PROJECTILE_ABILITY,
    MAGIC,
    THORNS,
    BLAST,
    FIRE,
    FALL,
    AILMENT,
    TRUE,
    OTHER;

    public static DamageType getType(EntityDamageEvent.DamageCause cause) {
        // List every cause for completeness
        return switch (cause) {
            case WORLD_BORDER, CONTACT, MELTING, DROWNING, STARVATION, LIGHTNING, FALLING_BLOCK, CUSTOM, DRYOUT,
                 FREEZE, CRAMMING, SONIC_BOOM, SUFFOCATION -> OTHER;
            case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK -> MELEE;
            case PROJECTILE -> PROJECTILE;
            case MAGIC -> MAGIC;
            case THORNS -> THORNS;
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> BLAST;
            case FIRE, FIRE_TICK, HOT_FLOOR, LAVA -> FIRE;
            case FALL, FLY_INTO_WALL -> FALL;
            case POISON, WITHER -> AILMENT;
            case VOID, KILL, SUICIDE -> TRUE;
            // we should log an error on default, this makes porting easier since any new damage types added will
            // automatically lead to a stacktrace
            default -> {
                RogueSmpCore.LOGGER.warn("Unknown/new damage type: {}", cause);
                yield OTHER;
            }
        };
    }

    public static boolean isMeleeDamage(DamageType type) {
        return type == MELEE || type == MELEE_ABILITY;
    }

    public static boolean isProjectileDamage(DamageType type) {
        return type == PROJECTILE || type == PROJECTILE_ABILITY;
    }
}
