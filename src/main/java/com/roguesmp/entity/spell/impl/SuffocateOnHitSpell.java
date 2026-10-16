package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.entity.spell.common.HitCondition;
import com.roguesmp.entity.spell.common.OnHitSpell;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.Utils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;

/**
 * Drains the victim's air supply (max 300). Once it runs out, each further hit deals {@code drownDamage}.
 */
public class SuffocateOnHitSpell extends OnHitSpell {

    public static final String TYPE_KEY = "suffocate_on_hit_spell";

    public record Params(HitCondition condition, int airDrain, double drownDamage) implements SpellParams {
        public static final Codec<Params> CODEC = Codec.composite(
                HitCondition.MAP_CODEC.forGetter(Params::condition),
                Codec.INT.optionalFieldOf("airDrain", 150).forGetter(Params::airDrain),
                Codec.DOUBLE.optionalFieldOf("drownDamage", 2.0).forGetter(Params::drownDamage),
                Params::new
        );

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    private final int airDrain;
    private final double drownDamage;

    public SuffocateOnHitSpell(LivingEntity owner, Params params) {
        super(owner, params.condition());
        this.airDrain = params.airDrain();
        this.drownDamage = params.drownDamage();
    }

    @Override
    protected void onHit(LivingEntity victim, DamageEvent event) {
        int remainingAir = victim.getRemainingAir() - airDrain;
        victim.setRemainingAir(Math.max(0, remainingAir));
        victim.getWorld().spawnParticle(Particle.BUBBLE_POP, victim.getEyeLocation(), 15, 0.3, 0.3, 0.3, 0.05);
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT_DROWN, 1f, 1f);

        if (remainingAir <= 0 && drownDamage > 0) {
            Utils.runLater(() -> {
                DamageEvent.Metadata metadata = DamageEvent.Metadata.spell(DamageType.OTHER, TYPE_KEY);
                metadata.setIgnoreIframe(true);
                metadata.setDoKnockback(false);
                DamageUtils.damage(victim, owner, drownDamage, metadata);
            });
        }
    }

    public static SuffocateOnHitSpell create(Params params, LivingEntity owner) {
        return new SuffocateOnHitSpell(owner, params);
    }
}
