package com.roguesmp.entity.spell.impl;

import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.impl.VanillaPotionEffect;
import com.roguesmp.codec.Codec;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellContext;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.entity.spell.common.SpellTargets;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Sudden Speed burst once the owner's target comes within {@code triggerRange}.
 */
public class SpeedBurstSpell extends Spell {

    public record Params(double triggerRange, int amplifier, int durationTicks, int cooldownTicks) implements SpellParams {
        public static final Codec<Params> CODEC = Codec.composite(
                Codec.DOUBLE.optionalFieldOf("triggerRange", 5.0).forGetter(Params::triggerRange),
                Codec.INT.optionalFieldOf("amplifier", 2).forGetter(Params::amplifier),
                Codec.INT.optionalFieldOf("durationTicks", 40).forGetter(Params::durationTicks),
                Codec.INT.optionalFieldOf("cooldownTicks", 160).forGetter(Params::cooldownTicks),
                Params::new
        );
    }

    private final LivingEntity owner;
    private final Params params;
    private int cooldownRemaining;

    public SpeedBurstSpell(LivingEntity owner, Params params) {
        this.owner = owner;
        this.params = params;
    }

    @Override
    public void tick(SpellContext context, int interval) {
        cooldownRemaining -= interval;
        if (cooldownRemaining > 0) return;

        Player target = SpellTargets.playerTarget(owner);
        if (target == null || target.getLocation().distanceSquared(owner.getLocation()) > params.triggerRange() * params.triggerRange()) return;

        cooldownRemaining = params.cooldownTicks();
        EffectManager.getInstance().addEffect(owner, VanillaPotionEffect.sourceOf(PotionEffectType.SPEED),
                new VanillaPotionEffect(params.durationTicks(), PotionEffectType.SPEED, params.amplifier()));
        owner.getWorld().spawnParticle(Particle.CLOUD, owner.getLocation().add(0, 0.2, 0), 10, 0.3, 0.1, 0.3, 0.05);
        owner.getWorld().playSound(owner.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 1f, 1.6f);
    }
}
