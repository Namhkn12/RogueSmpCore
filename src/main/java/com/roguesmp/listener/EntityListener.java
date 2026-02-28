package com.roguesmp.listener;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.roguesmp.constant.Keys;
import com.roguesmp.entity.BaseEntity;
import com.roguesmp.entity.EntityManager;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.event.SpellCastEvent;
import com.roguesmp.registry.EntityRegistry;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.persistence.PersistentDataType;

public class EntityListener implements Listener {
    private final EntityManager entityManager;
    private final EntityRegistry entityRegistry;

    public EntityListener(EntityManager entityManager, EntityRegistry entityRegistry) {
        this.entityManager = entityManager;
        this.entityRegistry = entityRegistry;
    }

    @EventHandler
    public void onAddToWorld(EntityAddToWorldEvent event) {
        Entity entity = event.getEntity();
        String entityId = entity.getPersistentDataContainer().get(Keys.MOB_ID, PersistentDataType.STRING);
        if (entityId == null) return;
        BaseEntity base = entityRegistry.getBaseEntity(entityId);
        if (base == null) return;
        if (!(entity instanceof LivingEntity living)) return;
        SmpEntity smpEntity = new SmpEntity(base, living);
        base.processSpell(smpEntity);
        entityManager.register(smpEntity);
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

}
