package com.roguesmp.entity.spell;

import com.roguesmp.event.DamageEvent;
import com.roguesmp.event.SpellCastEvent;
import org.bukkit.event.entity.*;

public abstract class Spell {

    public void tick(SpellContext context, int interval) {

    }

    public void onRemoved() {

    }

    public void onDamage(DamageEvent event) {

    }

    public void onHurt(DamageEvent event) {

    }

    public void onDeath(EntityDeathEvent event) {

    }

    public void onProjectileLaunch(ProjectileLaunchEvent event) {

    }

    public void onProjectileHit(ProjectileHitEvent event) {

    }

    public void onCastSpell(SpellCastEvent event) {

    }

    public void onTargetEntity(EntityTargetLivingEntityEvent event) {

    }

    public boolean hasNearbyPlayerDeathTrigger() {
        return false;
    }

    public void onNearbyPlayerDeath(PlayerDeathEvent event) {

    }
}
