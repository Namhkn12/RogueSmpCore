package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.entity.spell.common.HitCondition;
import com.roguesmp.entity.spell.common.OnHitSpell;
import com.roguesmp.entity.spell.common.SpellTargets;
import com.roguesmp.event.DamageEvent;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class HungerDrainOnHitSpell extends OnHitSpell {

    public record Params(HitCondition condition, int foodDrain, float saturationDrain, boolean negateDamage) implements SpellParams {
        public static final Codec<Params> CODEC = Codec.composite(
                HitCondition.MAP_CODEC.forGetter(Params::condition),
                Codec.INT.optionalFieldOf("foodDrain", 2).forGetter(Params::foodDrain),
                Codec.FLOAT.optionalFieldOf("saturationDrain", 2f).forGetter(Params::saturationDrain),
                Codec.BOOLEAN.optionalFieldOf("negateDamage", true).forGetter(Params::negateDamage),
                Params::new
        );
    }

    private final int foodDrain;
    private final float saturationDrain;
    private final boolean negateDamage;

    public HungerDrainOnHitSpell(LivingEntity owner, Params params) {
        super(owner, params.condition());
        this.foodDrain = params.foodDrain();
        this.saturationDrain = params.saturationDrain();
        this.negateDamage = params.negateDamage();
    }

    @Override
    protected void onHit(LivingEntity victim, DamageEvent event) {
        if (!(victim instanceof Player player)) return;
        if (negateDamage) event.setCancelled(true);

        player.setSaturation(Math.max(0, player.getSaturation() - saturationDrain));
        player.setFoodLevel(Math.max(0, player.getFoodLevel() - foodDrain));
        player.getWorld().spawnParticle(Particle.ITEM_SLIME, SpellTargets.centerOf(player), 12, 0.3, 0.4, 0.3, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 0.6f);
    }
}
