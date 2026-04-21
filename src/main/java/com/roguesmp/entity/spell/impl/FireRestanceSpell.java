package com.roguesmp.entity.spell.impl;

import com.roguesmp.entity.spell.Spell;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;

public class FireRestanceSpell extends Spell {

    private final LivingEntity owner;

    public FireRestanceSpell(LivingEntity owner) {
        this.owner = owner;
    }

    @Override
    public void run() {
        owner.addPotionEffects(List.of(
                new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 500, 1)
        ));
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    public static FireRestanceSpell readParam(Map<String, Object> param, LivingEntity owner) {
        return new FireRestanceSpell(owner);
    }
}
