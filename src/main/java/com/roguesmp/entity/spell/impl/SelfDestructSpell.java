package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellParams;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

public class SelfDestructSpell extends Spell {

    public static final String TYPE_KEY = "self_destruct_spell";
    public static final Integer DEFAULT_COUNT = 10;

    public record Params(int particleCount) implements SpellParams {
        public static final Codec<Params> CODEC = Codec.INT.optionalFieldOf("particleCount", DEFAULT_COUNT)
                .xmap(Params::new, Params::particleCount).codec();

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

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

    public static SelfDestructSpell create(Params params, LivingEntity owner) {
        return new SelfDestructSpell(params.particleCount());
    }
}
