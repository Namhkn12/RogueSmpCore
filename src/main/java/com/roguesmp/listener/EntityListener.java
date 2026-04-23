package com.roguesmp.listener;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.roguesmp.entity.EntityManager;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.event.SpellCastEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;

public class EntityListener implements Listener {
    private final EntityManager entityManager;

    public EntityListener(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @EventHandler
    public void onAddToWorld(EntityAddToWorldEvent event) {
        Entity entity = event.getEntity();
        entityManager.onAddToWorld(entity);
    }

    @EventHandler
    public void onRemoveFromWorld(EntityRemoveFromWorldEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity living)) return;
        SmpEntity smpEntity = entityManager.getSmpEntity(living);
        if (smpEntity != null) {
            entityManager.unload(event.getEntity());
        }
    }

    @EventHandler
    public void onDamage(DamageEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof LivingEntity living) {
                SmpEntity smpEntity = entityManager.getSmpEntity(living);
                if (smpEntity != null) smpEntity.onDamage(event);
            }
        } else if (damager instanceof LivingEntity living) {
            SmpEntity smpEntity = entityManager.getSmpEntity(living);
            if (smpEntity != null) smpEntity.onDamage(event);
        }
    }

    /*
     * Entity was hurt
     */
    @EventHandler
    public void onHurt(DamageEvent event) {
        if (event.getVictim() instanceof LivingEntity living) {
            SmpEntity smpEntity = entityManager.getSmpEntity(living);
            if (smpEntity != null) smpEntity.onHurt(event);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        SmpEntity smpEntity = entityManager.getSmpEntity(event.getEntity());
        if (smpEntity != null) {
            event.getDrops().clear(); //We will handle it ourselves
            smpEntity.onDeath(event);
            entityManager.unload(event.getEntity());
        }
    }

    /*
     * Entity shot a projectile
     */
    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof LivingEntity living) {
            SmpEntity smpEntity = entityManager.getSmpEntity(living);
            if (smpEntity != null) smpEntity.onProjectileLaunch(event);
        }
    }

    /*
     * Entity-shot projectile hit something
     */
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity().getShooter() instanceof LivingEntity living) {
            SmpEntity smpEntity = entityManager.getSmpEntity(living);
            if (smpEntity != null) smpEntity.onProjectileHit(event);
        }
    }

    @EventHandler
    public void onCastSpell(SpellCastEvent event) {
        event.getSmpEntity().onCastSpell(event);
    }

    // Yes, disable slime split since it's annoying to handle
    @EventHandler
    public void onSplit(SlimeSplitEvent event) {
        event.setCancelled(true);
    }

}
