package com.roguesmp.listener;

import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
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
        }

    }
}
