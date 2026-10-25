package com.roguesmp.entity.spell.impl;

import com.roguesmp.entity.spell.ActiveSpell;
import com.roguesmp.entity.spell.SpellContext;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class ShadowStepSpell extends ActiveSpell {

    private final LivingEntity owner;

    public ShadowStepSpell(LivingEntity owner) {
        super(50);
        this.owner = owner;
    }

    @Override
    public void onCast(SpellContext context) {
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
}
