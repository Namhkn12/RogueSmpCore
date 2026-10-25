package com.roguesmp.entity.spell.impl;

import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.impl.VanillaPotionEffect;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

public class FireRestanceSpell extends Spell {

    private final LivingEntity owner;

    public FireRestanceSpell(LivingEntity owner) {
        this.owner = owner;
    }

    @Override
    public void tick(SpellContext context, int interval) {
        EffectManager.getInstance().addEffect(owner, VanillaPotionEffect.sourceOf(PotionEffectType.FIRE_RESISTANCE),
                new VanillaPotionEffect(500, PotionEffectType.FIRE_RESISTANCE, 1));
    }
}
