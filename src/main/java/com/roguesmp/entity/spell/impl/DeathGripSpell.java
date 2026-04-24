package com.roguesmp.entity.spell.impl;

import com.roguesmp.entity.spell.Spell;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Map;

public class DeathGripSpell extends Spell {

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

    public static DeathGripSpell readParam(Map<String, Object> param, LivingEntity owner) {
        return new DeathGripSpell(owner);
    }
}
