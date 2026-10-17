package com.roguesmp.entity.spell.common;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.Utils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Shared gate for every {@link OnHitSpell}, flattened into the spell's own params JSON as
 * {@code chance} (0..1), {@code trigger} (MELEE/PROJECTILE/ANY) and {@code playersOnly}.
 */
public record HitCondition(double chance, HitTrigger trigger, boolean playersOnly) {

    public static final MapCodec<HitCondition> MAP_CODEC = Codec.composite(
            Codec.DOUBLE.optionalFieldOf("chance", 1.0).forGetter(HitCondition::chance),
            Codec.enumOf(HitTrigger.class).optionalFieldOf("trigger", HitTrigger.ANY).forGetter(HitCondition::trigger),
            Codec.BOOLEAN.optionalFieldOf("playersOnly", true).forGetter(HitCondition::playersOnly),
            HitCondition::new
    );

    public boolean test(DamageEvent event) {
        if (event.isCancelled()) return false;
        if (!trigger.matches(event)) return false;
        if (!(event.getVictim() instanceof LivingEntity)) return false;
        if (playersOnly && !(event.getVictim() instanceof Player)) return false;
        return chance >= 1 || Utils.RANDOM.nextDouble() < chance;
    }
}
