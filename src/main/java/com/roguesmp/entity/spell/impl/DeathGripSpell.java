package com.roguesmp.entity.spell.impl;

import com.roguesmp.entity.spell.ActiveSpell;
import com.roguesmp.entity.spell.SpellContext;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DeathGripSpell extends ActiveSpell {

    private final LivingEntity owner;

    public DeathGripSpell(LivingEntity owner) {
        super(60);
        this.owner = owner;
    }

    @Override
    public void onCast(SpellContext context) {
        Entity target = owner.getTargetEntity(20);
        if (!(target instanceof Player player)) return;

        Location center = owner.getLocation();
        int radius = 3;

        double angle = Math.random() * 2 * Math.PI;
        double x = center.getX() + radius * Math.cos(angle);
        double z = center.getZ() + radius * Math.sin(angle);

        player.teleport(new Location(center.getWorld(), x, center.getY(), z));
    }
}
