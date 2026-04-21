package com.roguesmp.entity.spell.impl;

import com.roguesmp.entity.spell.Spell;
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
        Collection<Player> players = owner.getLocation().getNearbyPlayers(5);
        players.forEach(player -> {
            player.addPotionEffects(List.of(
                    new PotionEffect(PotionEffectType.BLINDNESS, 60, 4)
            ));
        });
    }

    @Override
    public int cooldownTicks() {
        return 7;
    }

    public static BlindSpell readParam(Map<String, Object> param, LivingEntity owner) {
        return new BlindSpell(owner);
    }
}
