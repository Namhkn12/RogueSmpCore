package com.roguesmp.entity.spell.impl;

import com.roguesmp.entity.spell.Spell;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Map;

public class SelfDestructSpell extends Spell {

    public static final Integer DEFAULT_COUNT = 10;
    private int particleCount;

    public SelfDestructSpell(int particleCount) {
        this.particleCount = particleCount;
    }

    @Override
    public void run(int interval) {

    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    @Override
    public void onDeath(EntityDeathEvent event) {
        event.setDeathSound(Sound.ENTITY_GENERIC_EXPLODE);
        Particle.EXPLOSION.builder().location(event.getEntity().getLocation()).receivers(10).count(particleCount).spawn();
    }

    public static SelfDestructSpell readParam(Map<String, Object> data, LivingEntity owner) {
        if (data == null) return new SelfDestructSpell(DEFAULT_COUNT);
        Long count = (Long) data.get("particleCount"); //Gson quirk that make whole number return as Long
        if (count == null) return new SelfDestructSpell(DEFAULT_COUNT);
        return new SelfDestructSpell(Math.toIntExact(count));
    }
}
