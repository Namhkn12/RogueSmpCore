package com.roguesmp.listener;

import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageDisplayUtils;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.EntityUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

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

            if (damager instanceof EvokerFangs fangs) {
                if (fangs.getOwner() != null) {
                    double damage = EntityUtils.getAttributeOrDefault(fangs.getOwner(), Attribute.ATTACK_DAMAGE, 0);
                    event.setDamage(damage);
                }
            }

            DamageEvent damageEvent;
            double originalDamage = event.getDamage();
            if (DamageUtils.nextMetadata != null) { // Damage caused by plugin via DamageUtils
                damageEvent = new DamageEvent(victim, damager, originalDamage, DamageUtils.nextMetadata);
                DamageUtils.nextMetadata = null;
            } else {
                DamageType damageType = DamageType.getType(event.getCause());
                damageEvent = new DamageEvent(victim, damager, originalDamage, new DamageEvent.Metadata(damageType));
            }

            damageEvent.setCritical(entityDamageByEntityEvent.isCritical());
            Bukkit.getPluginManager().callEvent(damageEvent);

            event.setCancelled(damageEvent.isCancelled());
            event.setDamage(damageEvent.getFinalDamage());
        } else {
            Entity victim = event.getEntity();
            if (victim instanceof Item) return; //Somehow item burning also call damage event

            DamageEvent damageEvent;
            if (DamageUtils.nextMetadata != null) { // Damage caused by plugin via DamageUtils
                damageEvent = new DamageEvent(victim, null, event.getDamage(), DamageUtils.nextMetadata);
                DamageUtils.nextMetadata = null;
            } else {
                DamageType damageType = DamageType.getType(event.getCause());
                damageEvent = new DamageEvent(victim, null, event.getDamage(), new DamageEvent.Metadata(damageType));
            }

            Bukkit.getPluginManager().callEvent(damageEvent);
            event.setCancelled(damageEvent.isCancelled());
            event.setDamage(damageEvent.getFinalDamage());
        }

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void spawnDisplay(DamageEvent event) {
        if (event.isCancelled()) return;

        Entity victim = event.getVictim();
        Entity attacker = event.getDamager();

        // Start a bit above the victim's chest-center — high enough to clear the model, low enough not to float off the head
        Location spawnLoc = victim.getLocation().add(0, victim.getHeight() * 0.6, 0);

        // Identify which player needs to see the display (attacker takes priority)
        Player viewer = null;
        if (attacker instanceof Player player) {
            viewer = player;
        } else if (attacker instanceof Projectile projectile && projectile.getShooter() instanceof Player pl) {
            viewer = pl;
        } else if (victim instanceof Player player) {
            viewer = player;
        }

        if (viewer != null) {
            // Line-of-sight calculation: Move the spawn point slightly towards the player
            Location playerEyeLoc = viewer.getEyeLocation();
            Vector directionToPlayer = playerEyeLoc.toVector().subtract(spawnLoc.toVector());

            // Distance factor: 0.0 = at victim, 1.0 = at player.
            // Pulled further toward the viewer than the victim's own position so it reads as popping
            // out at the camera instead of sitting flush on the model.
            double distanceFactor = 0.5;
            spawnLoc.add(directionToPlayer.multiply(distanceFactor));

            // Add a slight random spread so overlapping damage numbers don't stack perfectly
            double spread = 1.1;
            spawnLoc.add(
                    (Utils.RANDOM.nextDouble() - 0.5) * spread,
                    (Utils.RANDOM.nextDouble() - 0.5) * spread,
                    (Utils.RANDOM.nextDouble() - 0.5) * spread
            );
        } else {
            // Fallback offset for non-player damage (e.g. mob vs mob) — there's no viewer to pop
            // toward here, so lean on a wider jitter instead to stay comparably prominent.
            double spread = 1.3;
            double offsetX = (Utils.RANDOM.nextDouble() - 0.5) * spread;
            double offsetY = (Utils.RANDOM.nextDouble() - 0.5) * spread;
            double offsetZ = (Utils.RANDOM.nextDouble() - 0.5) * spread;
            spawnLoc = victim.getLocation().add(offsetX, victim.getHeight() * 0.6 + offsetY, offsetZ);
        }

        DamageDisplayUtils.spawnDamageDisplay(
                spawnLoc,
                event.getFinalDamage(),
                event.getDamageType(),
                event.isCritical()
        );
    }
}
