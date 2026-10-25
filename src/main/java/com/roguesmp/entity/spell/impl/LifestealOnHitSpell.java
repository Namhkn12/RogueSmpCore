package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.entity.spell.common.HitCondition;
import com.roguesmp.entity.spell.common.OnHitSpell;
import com.roguesmp.entity.spell.common.SpellTargets;
import com.roguesmp.event.DamageEvent;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

public class LifestealOnHitSpell extends OnHitSpell {

    public record Params(HitCondition condition, double missingHealthRatio, double flatHeal) implements SpellParams {
        public static final Codec<Params> CODEC = Codec.composite(
                HitCondition.MAP_CODEC.forGetter(Params::condition),
                Codec.DOUBLE.optionalFieldOf("missingHealthRatio", 0.5).forGetter(Params::missingHealthRatio),
                Codec.DOUBLE.optionalFieldOf("flatHeal", 0.0).forGetter(Params::flatHeal),
                Params::new
        );
    }

    private final double missingHealthRatio;
    private final double flatHeal;

    public LifestealOnHitSpell(LivingEntity owner, Params params) {
        super(owner, params.condition());
        this.missingHealthRatio = params.missingHealthRatio();
        this.flatHeal = params.flatHeal();
    }

    @Override
    protected void onHit(LivingEntity victim, DamageEvent event) {
        AttributeInstance maxHealthAttribute = owner.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute == null || !owner.isValid()) return;

        double maxHealth = maxHealthAttribute.getValue();
        double missingHealth = maxHealth - owner.getHealth();
        if (missingHealth <= 0) return;

        double heal = missingHealth * missingHealthRatio + flatHeal;
        owner.setHealth(Math.min(maxHealth, owner.getHealth() + heal));
        owner.getWorld().spawnParticle(Particle.HEART, SpellTargets.centerOf(owner).add(0, owner.getHeight() * 0.6, 0), 3, 0.3, 0.2, 0.3);
    }
}
