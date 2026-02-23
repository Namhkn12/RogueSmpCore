package com.roguesmp.listener;

import com.roguesmp.constant.EquipSlot;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.item.SmpItem;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.PlayerProjectile;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final PlayerManager playerManager;

    public PlayerListener(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerManager.addPlayer(player.getUniqueId());

    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playerManager.removePlayer(player.getUniqueId());
    }

    @EventHandler
    public void onEquipmentChange(EntityEquipmentChangedEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
        event.getEquipmentChanges().forEach((equipmentSlot, equipmentChange) -> {
            EquipSlot equipSlot = EquipSlot.fromVanilla(equipmentSlot.getGroup());
            if (smpPlayer != null) {
                SmpItem newItem = null;
                if (ItemStackUtils.isValidItem(equipmentChange.newItem())) {
                    newItem = new SmpItem(equipmentChange.newItem());
                    player.getEquipment().setItem(equipmentSlot, newItem.generateItemStack(smpPlayer, equipmentChange.newItem().getAmount()));
                }

                smpPlayer.updateSlotStat(player, equipSlot, new SmpItem(equipmentChange.oldItem()), newItem);
            }
        });
    }

    @EventHandler
    public void onCustomDamage(DamageEvent event) {
        Entity victim = event.getVictim();
        Entity damager = event.getDamager();
        if (victim instanceof Player player) {
            // Player got damaged
            SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
            if (smpPlayer == null) return;
            smpPlayer.getActiveEnchants().forEach((enchants, integer) -> {
                enchants.getEnchant().onHurt(event, integer, smpPlayer);
            });
            smpPlayer.getActiveAttributes().forEach((attributes, aDouble) -> {
                attributes.getAttribute().onHurt(event, aDouble, smpPlayer);
            });
        }

        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            // Player damage an entity with projectile
            SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
            if (smpPlayer == null) return;
            PlayerProjectile playerProjectile = smpPlayer.getProjectile(projectile.getUniqueId());
            if (playerProjectile == null) return;
            playerProjectile.getActiveEnchants().forEach((enchants, integer) -> {
                enchants.getEnchant().onDamageEntity(event, integer, smpPlayer);
            });
            playerProjectile.getActiveAttributes().forEach((attributes, aDouble) -> {
                attributes.getAttribute().onDamageEntity(event, aDouble, smpPlayer);
            });
            return;
        }

        if (damager instanceof Player player) {
            // Player damage (melee) an entity
            SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
            if (smpPlayer == null) return;
            smpPlayer.getActiveEnchants().forEach((enchants, integer) -> {
                enchants.getEnchant().onDamageEntity(event, integer, smpPlayer);
            });
            smpPlayer.getActiveAttributes().forEach((attributes, aDouble) -> {
                attributes.getAttribute().onDamageEntity(event, aDouble, smpPlayer);
            });
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST) // Should run last to catch final damage dealt
    public void onHurtFatal(DamageEvent event) {
        if (event.getVictim() instanceof Player player && event.getFinalDamage() >= player.getHealth()) {
            SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
            if (smpPlayer == null) return;
            smpPlayer.getActiveEnchants().forEach((enchants, integer) -> {
                enchants.getEnchant().onHurtFatal(event, integer, smpPlayer);
            });
            smpPlayer.getActiveAttributes().forEach((attributes, aDouble) -> {
                attributes.getAttribute().onHurtFatal(event, aDouble, smpPlayer);
            });
        }
    }

    @EventHandler
    public void onKillEntity(EntityDeathEvent event) {
        Entity entity = event.getDamageSource().getCausingEntity();
        if (!(entity instanceof Player player)) return;
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
        if (smpPlayer == null) return;
        smpPlayer.getActiveEnchants().forEach((enchants, integer) -> {
            enchants.getEnchant().onKillEntity(event, integer, smpPlayer);
        });
        smpPlayer.getActiveAttributes().forEach((attributes, aDouble) -> {
            attributes.getAttribute().onKillEntity(event, aDouble, smpPlayer);
        });
    }

    @EventHandler
    public void onConsumeItem(PlayerItemConsumeEvent event) {
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(event.getPlayer().getUniqueId());
        if (smpPlayer == null) return;
        smpPlayer.getActiveEnchants().forEach((enchants, integer) -> {
            enchants.getEnchant().onConsume(event, integer, smpPlayer);
        });
        smpPlayer.getActiveAttributes().forEach((attributes, aDouble) -> {
            attributes.getAttribute().onConsume(event, aDouble, smpPlayer);
        });
    }

    @EventHandler
    public void onExpChange(PlayerExpChangeEvent event) {
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(event.getPlayer().getUniqueId());
        if (smpPlayer == null) return;
        smpPlayer.getActiveEnchants().forEach((enchants, integer) -> {
            enchants.getEnchant().onExpChange(event, integer, smpPlayer);
        });
        smpPlayer.getActiveAttributes().forEach((attributes, aDouble) -> {
            attributes.getAttribute().onExpChange(event, aDouble, smpPlayer);
        });
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(event.getPlayer().getUniqueId());
        if (smpPlayer == null) return;
        smpPlayer.getActiveEnchants().forEach((enchants, integer) -> {
            enchants.getEnchant().onBlockBreak(event, integer, smpPlayer);
        });
        smpPlayer.getActiveAttributes().forEach((attributes, aDouble) -> {
            attributes.getAttribute().onBlockBreak(event, aDouble, smpPlayer);
        });
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
        if (smpPlayer == null) return;
        PlayerProjectile playerProjectile = smpPlayer.getProjectile(event.getEntity().getUniqueId());
        if (playerProjectile == null) return;
        playerProjectile.getActiveEnchants().forEach((enchants, integer) -> {
            enchants.getEnchant().onProjectileHit(event, integer, smpPlayer);
        });
        playerProjectile.getActiveAttributes().forEach((attributes, aDouble) -> {
            attributes.getAttribute().onProjectileHit(event, aDouble, smpPlayer);
        });
        if (event.getHitBlock() != null) {
            Utils.runLater(() -> smpPlayer.untrackProjectile(event.getEntity().getUniqueId()));
        }

    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
        if (smpPlayer == null) return;
        event.getEntity().setPersistent(false);
        smpPlayer.trackProjectile(event.getEntity());
        PlayerProjectile playerProjectile = smpPlayer.getProjectile(event.getEntity().getUniqueId());
        if (playerProjectile == null) return;
        playerProjectile.getActiveEnchants().forEach((enchants, integer) -> {
            enchants.getEnchant().onProjectileLaunch(event, integer, smpPlayer);
        });
        playerProjectile.getActiveAttributes().forEach((attributes, aDouble) -> {
            attributes.getAttribute().onProjectileLaunch(event, aDouble, smpPlayer);
        });
        player.sendMessage(String.valueOf(event.getEntity().getVelocity().length()));
        // Untrack in case the projectile never hit anything
        Utils.runLater(() -> smpPlayer.untrackProjectile(event.getEntity().getUniqueId()), 20 * 10);
    }
}
