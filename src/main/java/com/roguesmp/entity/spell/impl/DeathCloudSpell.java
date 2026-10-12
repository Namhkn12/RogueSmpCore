package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.entity.spell.common.CloudSpec;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

public class DeathCloudSpell extends Spell {

    public static final String TYPE_KEY = "death_cloud_spell";

    public record Params(CloudSpec cloud, boolean burstEffect) implements SpellParams {
        public static final Codec<Params> CODEC = Codec.composite(
                CloudSpec.CODEC.fieldOf("cloud").forGetter(Params::cloud),
                Codec.BOOLEAN.optionalFieldOf("burstEffect", true).forGetter(Params::burstEffect),
                Params::new
        );

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    private final LivingEntity owner;
    private final Params params;

    public DeathCloudSpell(LivingEntity owner, Params params) {
        this.owner = owner;
        this.params = params;
    }

    @Override
    public void onDeath(EntityDeathEvent event) {
        Location location = owner.getLocation();
        params.cloud().spawn(location, owner);
        if (params.burstEffect()) {
            location.getWorld().spawnParticle(Particle.EXPLOSION, location.clone().add(0, 0.5, 0), 1);
            location.getWorld().playSound(location, Sound.ENTITY_PUFFER_FISH_BLOW_OUT, 1.2f, 0.6f);
        }
    }

    @Override
    public void run(int interval) {
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    public static DeathCloudSpell create(Params params, LivingEntity owner) {
        return new DeathCloudSpell(owner, params);
    }
}
