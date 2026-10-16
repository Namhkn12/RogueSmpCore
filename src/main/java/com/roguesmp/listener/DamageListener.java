package com.roguesmp.listener;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.roguesmp.constant.DamageFlag;
import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.death.LastHit;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class DamageListener implements Listener {
    private record HitWindow(int startTick, double rawDamage) {
        boolean isActive(LivingEntity victim) {
            return Bukkit.getCurrentTick() - startTick < victim.getMaximumNoDamageTicks() / 2;
        }
    }

    private static final Map<UUID, HitWindow> HIT_WINDOWS = new HashMap<>();

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

            DamageEvent damageEvent = new DamageEvent(victim, damager, event.getDamage(), resolveMetadata(event));
            damageEvent.setCritical(entityDamageByEntityEvent.isCritical());
            dispatch(event, damageEvent);
        } else {
            Entity victim = event.getEntity();
            if (victim instanceof Item) return;

            dispatch(event, new DamageEvent(victim, null, event.getDamage(), resolveMetadata(event)));
        }
    }

    private static DamageEvent.Metadata resolveMetadata(EntityDamageEvent event) {
        if (DamageType.isPluginSource(event.getDamageSource())) {
            DamageEvent.Metadata metadata = DamageUtils.consumeMetadata();
            if (metadata != null) return metadata;
        }
        DamageType type = DamageType.fromSource(event.getDamageSource());
        return event.getDamageSource().getCausingEntity() != null
                ? DamageEvent.Metadata.attack(type)
                : DamageEvent.Metadata.environment(type);
    }

    private static void dispatch(EntityDamageEvent event, DamageEvent damageEvent) {
        damageEvent.setDamageSource(event.getDamageSource());
        double raw = event.getDamage();
        double invulnerabilityRatio = getInvulnerabilityRatio(event, raw);
        if (invulnerabilityRatio <= 0) {
            event.setCancelled(true);
            return;
        }
        if (invulnerabilityRatio < 1) {
            damageEvent.addDamageModifier(invulnerabilityRatio, DamageOperation.MORE_FINAL);
        }
        damageEvent.setShieldBlockRatio(getShieldBlockRatio(event, raw));

        Bukkit.getPluginManager().callEvent(damageEvent);
        if (damageEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }
        recordHit(event, raw);
        event.setDamage(damageEvent.getFinalDamage());

        if (event.getEntity() instanceof Player player) {
            SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
            if (smpPlayer != null) {
                smpPlayer.setLastHit(new LastHit(damageEvent.getMetadata(), event.getDamageSource().getCausingEntity(), Bukkit.getCurrentTick()));
            }
        }
    }

    private static double getInvulnerabilityRatio(EntityDamageEvent event, double raw) {
        if (!(event.getEntity() instanceof LivingEntity living) || raw <= 0) return 1;
        if (DamageType.hasFlag(event.getDamageSource(), DamageFlag.BYPASS_COOLDOWN)) return 1;

        HitWindow window = HIT_WINDOWS.get(living.getUniqueId());
        if (window == null || !window.isActive(living)) return 1;
        if (raw <= window.rawDamage()) return 0;
        return (raw - window.rawDamage()) / raw;
    }

    private static void recordHit(EntityDamageEvent event, double raw) {
        if (!(event.getEntity() instanceof LivingEntity living) || raw <= 0) return;

        UUID uuid = living.getUniqueId();
        HitWindow window = HIT_WINDOWS.get(uuid);
        boolean continuesWindow = window != null && window.isActive(living)
                && !DamageType.hasFlag(event.getDamageSource(), DamageFlag.BYPASS_COOLDOWN);
        int startTick = continuesWindow ? window.startTick() : Bukkit.getCurrentTick();
        HIT_WINDOWS.put(uuid, new HitWindow(startTick, raw));
    }

    private static double getShieldBlockRatio(EntityDamageEvent event, double raw) {
        if (raw <= 0 || !(event.getEntity() instanceof LivingEntity living)) return 0;

        double finalDamage = event.getFinalDamage();
        double absorption = living.getAbsorptionAmount();
        double afterBlock;
        if (finalDamage > 0) {
            afterBlock = finalDamage + absorption;
        } else {
            afterBlock = absorption > 0 && !(living instanceof HumanEntity human && human.isBlocking()) ? raw : 0;
        }
        return Math.clamp(1 - afterBlock / raw, 0, 1);
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        HIT_WINDOWS.remove(event.getEntity().getUniqueId());
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
