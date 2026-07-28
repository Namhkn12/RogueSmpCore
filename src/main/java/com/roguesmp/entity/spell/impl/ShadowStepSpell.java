package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellParams;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class ShadowStepSpell extends Spell {

    public static final String TYPE_KEY = "shadow_step_spell";

    public record Params() implements SpellParams {
        public static final Codec<Params> CODEC = MapCodec.unit(Params::new).codec();

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    private final LivingEntity owner;

    public ShadowStepSpell(LivingEntity owner) {
        this.owner = owner;
    }

    @Override
    public void run(int interval) {
        Entity target = owner.getTargetEntity(20);
        if (!(target instanceof Player player)) return;

        Vector behind = player.getLocation().getDirection().normalize().multiply(-1.5);
        Location teleportLoc = player.getLocation().add(behind);
        owner.getWorld().spawnParticle(
                Particle.PORTAL,
                owner.getLocation(),
                20,
                0.5, 1.0, 0.5,
                0.1
        );
        owner.teleport(teleportLoc);
    }

    @Override
    public int cooldownTicks() {
        return 50;
    }

    public static ShadowStepSpell create(Params params, LivingEntity owner) {
        return new ShadowStepSpell(owner);
    }
}
