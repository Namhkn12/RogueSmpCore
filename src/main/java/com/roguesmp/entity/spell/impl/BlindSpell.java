package com.roguesmp.entity.spell.impl;

import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.impl.VanillaPotionEffect;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellContext;
import com.roguesmp.event.DamageEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class BlindSpell extends Spell {

    private final LivingEntity owner;

    public BlindSpell(LivingEntity owner) {
        this.owner = owner;
    }

    @Override
    public void tick(SpellContext context, int interval) {
    }

    @Override
    public void onDamage(DamageEvent event) {
        Player player = (Player) event.getVictim();
        if (player != null) EffectManager.getInstance().addEffect(player, VanillaPotionEffect.sourceOf(PotionEffectType.BLINDNESS),
                new VanillaPotionEffect(60, PotionEffectType.BLINDNESS, 4));
    }
}
