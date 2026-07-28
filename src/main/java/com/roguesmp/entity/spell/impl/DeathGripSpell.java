package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellParams;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DeathGripSpell extends Spell {

    public static final String TYPE_KEY = "death_grip_spell";

    public record Params() implements SpellParams {
        public static final Codec<Params> CODEC = MapCodec.unit(Params::new).codec();

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    private final LivingEntity owner;

    public DeathGripSpell(LivingEntity owner) {
        this.owner = owner;
    }

    @Override
    public void run(int interval) {
        Entity target = owner.getTargetEntity(20);
        if (!(target instanceof Player player)) return;

        Location center = owner.getLocation();
        int radius = 3;

        double angle = Math.random() * 2 * Math.PI;
        double x = center.getX() + radius * Math.cos(angle);
        double z = center.getZ() + radius * Math.sin(angle);

        player.teleport(new Location(center.getWorld(), x, center.getY(), z));
    }

    @Override
    public int cooldownTicks() {
        return 60;
    }

    public static DeathGripSpell create(Params params, LivingEntity owner) {
        return new DeathGripSpell(owner);
    }
}
