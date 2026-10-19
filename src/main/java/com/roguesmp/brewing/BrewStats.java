package com.roguesmp.brewing;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.effect.impl.DamageIncreaseEffect;
import com.roguesmp.effect.impl.ResistanceEffect;
import com.roguesmp.effect.impl.VulnerabilityEffect;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Every brewable stat: one per {@link Attributes} constant (lowercase name), plus the effects
 * that have no attribute equivalent.
 */
public final class BrewStats {

    private static final Map<String, BrewStat> STATS = new HashMap<>();

    static {
        for (Attributes attribute : Attributes.values()) register(new BrewStat.AttributeStat(attribute));

        effect(VulnerabilityEffect.ID, VulnerabilityEffect::new);
        effect(DamageIncreaseEffect.ID, DamageIncreaseEffect::new);
        effect(ResistanceEffect.ID, (duration, value) -> new ResistanceEffect(duration, value, SmpEffect.DeathBehavior.REMOVE_ON_DEATH));
    }

    private BrewStats() {}

    public static @Nullable BrewStat get(String id) {
        return STATS.get(id);
    }

    private static void effect(String id, BiFunction<Integer, Double, SmpEffect> factory) {
        register(new BrewStat.EffectStat(id, (value, duration) -> factory.apply(duration, value)));
    }

    private static void register(BrewStat stat) {
        if (STATS.putIfAbsent(stat.id(), stat) != null) throw new IllegalStateException("Duplicate brew stat '" + stat.id() + "'");
    }
}
