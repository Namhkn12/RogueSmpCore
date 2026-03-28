package com.roguesmp.entity.spell.impl;

import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.impl.SpeedEffect;
import com.roguesmp.entity.spell.Spell;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;

public class SlowAuraSpell extends Spell {

    private final LivingEntity owner;

    public SlowAuraSpell(LivingEntity owner) {
        this.owner = owner;
    }

    @Override
    public void run() {
        Collection<Player> players = owner.getLocation().getNearbyPlayers(5);
        players.forEach(player -> {
            EffectManager.getInstance().addEffect(player, "slow_aura_spell", new SpeedEffect(100, -0.3, "slow_aura_spell"));
        });

    }

    @Override
    public int cooldownTicks() {
        return 5;
    }

    public static SlowAuraSpell readParam(Map<String, Object> param, LivingEntity owner) {
        return new SlowAuraSpell(owner);
    }
}
