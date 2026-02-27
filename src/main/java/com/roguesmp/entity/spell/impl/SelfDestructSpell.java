package com.roguesmp.entity.spell.impl;

import com.roguesmp.entity.spell.Spell;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Map;

public class SelfDestructSpell extends Spell {

    public static final Integer DEFAULT_COUNT = 10;
    private int particleCount;

    public SelfDestructSpell(int particleCount) {
        this.particleCount = particleCount;
    }

    @Override
    public void run() {

    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    @Override
    public void onDeath(EntityDeathEvent event) {
        event.setDeathSound(Sound.ENTITY_GENERIC_EXPLODE);
        Particle.EXPLOSION.builder().receivers(10).color(Color.RED).count(particleCount).spawn();
    }

    public static SelfDestructSpell readParam(Map<String, Object> data) {
        Integer count = (Integer) data.get("particleCount");
        if (count == null) return new SelfDestructSpell(DEFAULT_COUNT);
        return new SelfDestructSpell(count);
    }
}
