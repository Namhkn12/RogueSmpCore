package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.impl.BleedingEffect;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.entity.spell.common.HitCondition;
import com.roguesmp.entity.spell.common.OnHitSpell;
import com.roguesmp.event.DamageEvent;
import org.bukkit.entity.LivingEntity;

public class BleedOnHitSpell extends OnHitSpell {

    public static final String TYPE_KEY = "bleed_on_hit_spell";

    public record Params(HitCondition condition, int duration, double damagePerSecond) implements SpellParams {
        public static final Codec<Params> CODEC = Codec.composite(
                HitCondition.MAP_CODEC.forGetter(Params::condition),
                Codec.INT.optionalFieldOf("duration", 100).forGetter(Params::duration),
                Codec.DOUBLE.optionalFieldOf("damagePerSecond", 1.0).forGetter(Params::damagePerSecond),
                Params::new
        );
    }

    private final int duration;
    private final double damagePerSecond;

    public BleedOnHitSpell(LivingEntity owner, Params params) {
        super(owner, params.condition());
        this.duration = params.duration();
        this.damagePerSecond = params.damagePerSecond();
    }

    @Override
    protected void onHit(LivingEntity victim, DamageEvent event) {
        EffectManager.getInstance().addEffect(victim, TYPE_KEY, new BleedingEffect(duration, damagePerSecond, owner));
    }
}
