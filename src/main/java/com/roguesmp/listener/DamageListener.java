package com.roguesmp.listener;

import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class DamageListener implements Listener {
    @EventHandler
    public void onDamageEntity(EntityDamageEvent event) {
        if (event.isCancelled()) return;
        if (event instanceof EntityDamageByEntityEvent entityDamageByEntityEvent) {
            Entity damager = entityDamageByEntityEvent.getDamager();
            Entity victim = event.getEntity();
            if (damager instanceof Player && victim instanceof Player) {
                event.setCancelled(true);
                return;
            }
            if (damager instanceof Projectile projectile) {
                if (projectile.getShooter() instanceof Player) {
                    if (victim instanceof Player) {
                        event.setCancelled(true);
                        return;
                    }
                } else if (projectile.getShooter() instanceof LivingEntity living && !(projectile.getShooter() instanceof Player)) {
                    // Wither, skeleton, blaze, snow golem, etc... will use their attack damage attribute as projectil damage
                    // for easier entity creation
                    double projectileDamage = EntityUtils.getAttributeOrDefault(living, Attribute.ATTACK_DAMAGE, 0);
                    event.setDamage(projectileDamage);
                }
            }

            DamageEvent.Metadata metadata = DamageUtils.nextMetadata;
            DamageEvent damageEvent;
            if (metadata != null) { // Damage caused by plugin via DamageUtils
                damageEvent = new DamageEvent(victim, damager, event.getDamage(), metadata);
                DamageUtils.nextMetadata = null;
            } else {
                DamageType damageType = DamageType.getType(event.getCause());
                damageEvent = new DamageEvent(victim, damager, event.getDamage(), new DamageEvent.Metadata(damageType));
            }

            damageEvent.setCritical(entityDamageByEntityEvent.isCritical());
            Bukkit.getPluginManager().callEvent(damageEvent);

            event.setCancelled(damageEvent.isCancelled());
            event.setDamage(damageEvent.getFinalDamage());

            if (damager instanceof Player player) {
                player.sendMessage(damageEvent.getFinalDamage() + " " + damageEvent.getDamageType());
            } else if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
                player.sendMessage(damageEvent.getFinalDamage() + " " + damageEvent.getDamageType());
            }
        } else {
            Entity victim = event.getEntity();
            DamageType damageType = DamageType.getType(event.getCause());
            DamageEvent damageEvent = new DamageEvent(victim, null, event.getDamage(), new DamageEvent.Metadata(damageType));

            Bukkit.getPluginManager().callEvent(damageEvent);
            event.setCancelled(damageEvent.isCancelled());
            event.setDamage(damageEvent.getFinalDamage());
        }

    }
}
