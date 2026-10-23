package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellContext;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.entity.spell.common.PotionEffectSpec;
import com.roguesmp.utils.PlayerUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * Every {@code intervalTicks}, applies {@code effects} to players within {@code radius}. Effect
 * durations should exceed {@code intervalTicks} for the aura to feel continuous.
 */
public class EffectAuraSpell extends Spell {

    private static final List<PotionEffectSpec> DEFAULT_EFFECTS = List.of(new PotionEffectSpec(PotionEffectType.SLOWNESS, 40, 0));

    public record Params(double radius, List<PotionEffectSpec> effects, boolean requireLineOfSight, int intervalTicks) implements SpellParams {
        public static final Codec<Params> CODEC = Codec.composite(
                Codec.DOUBLE.optionalFieldOf("radius", 5.0).forGetter(Params::radius),
                PotionEffectSpec.LIST_CODEC.optionalFieldOf("effects", DEFAULT_EFFECTS).forGetter(Params::effects),
                Codec.BOOLEAN.optionalFieldOf("requireLineOfSight", true).forGetter(Params::requireLineOfSight),
                Codec.INT.optionalFieldOf("intervalTicks", 20).forGetter(Params::intervalTicks),
                Params::new
        );
    }

    private final LivingEntity owner;
    private final Params params;
    private int cooldownRemaining;

    public EffectAuraSpell(LivingEntity owner, Params params) {
        this.owner = owner;
        this.params = params;
    }

    @Override
    public void tick(SpellContext context, int interval) {
        cooldownRemaining -= interval;
        if (cooldownRemaining > 0) return;
        cooldownRemaining = params.intervalTicks();

        PlayerUtils.playersInRange(owner.getLocation(), params.radius(), false).stream()
                .filter(player -> !params.requireLineOfSight() || owner.hasLineOfSight(player))
                .forEach(player -> PotionEffectSpec.applyAll(player, params.effects()));
    }
}
