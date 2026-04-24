package com.roguesmp.entity.spell.impl;

import com.roguesmp.entity.spell.Spell;
import com.roguesmp.event.DamageEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class BlindSpell extends Spell {

    private final LivingEntity owner;

    public BlindSpell(LivingEntity owner) {
        this.owner = owner;
    }


    @Override
    public void run() {
    }

    @Override
    public void onDamage(DamageEvent event) {
        Player player = (Player) event.getVictim();
        if(player != null) player.addPotionEffects(List.of(
                new PotionEffect(PotionEffectType.BLINDNESS, 60, 4)
        ));
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    public static BlindSpell readParam(Map<String, Object> param, LivingEntity owner) {
        return new BlindSpell(owner);
    }
}
