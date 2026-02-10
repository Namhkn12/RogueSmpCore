package com.roguesmp.listener;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.context.DamageContext;
import com.roguesmp.item.SmpItem;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
                player.sendMessage("EquipmentChange " + equipSlot.name());
            }
        });
    }

    @EventHandler
    public void onDamageEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Projectile projectile) {
            if (!(projectile.getShooter() instanceof Player)) return;
            //Player projectiles (bow, trident, etc...) damage entity
        }

        if (!(event.getDamager() instanceof Player) && event.getEntity() instanceof Player player)  {
            //Other entity (including projectiles) damage player
            SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
            if (smpPlayer == null) return;
            DamageContext damageContext = new DamageContext(player, event.getDamager(), 1);
            smpPlayer.getActiveEnchants().forEach((enchants, integer) -> {
                enchants.getEnchant().onHurt(damageContext, integer, smpPlayer);
            });
            smpPlayer.getActiveAttributes().forEach((attributes, aDouble) -> {
                attributes.getAttribute().onHurt(damageContext, aDouble, smpPlayer);
            });

            if (damageContext.calculateFinalDamage() >= player.getHealth()) {
                smpPlayer.getActiveEnchants().forEach((enchants, integer) -> {
                    enchants.getEnchant().onHurtFatal(damageContext, integer, smpPlayer);
                });
                smpPlayer.getActiveAttributes().forEach((attributes, aDouble) -> {
                    attributes.getAttribute().onHurtFatal(damageContext, aDouble, smpPlayer);
                });
            }
            event.setCancelled(damageContext.isCancelled());
            event.setDamage(damageContext.calculateFinalDamage());
            event.getDamager().sendMessage(String.valueOf(event.getDamage()));

            return;
        }

        if (event.getDamager() instanceof Player player && (!(event.getEntity() instanceof Player))) {
            //Player (melee) damage other entities
            SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
            if (smpPlayer == null) return;
            DamageContext damageContext = new DamageContext(event.getEntity(), player, 1);
            damageContext.setCritical(event.isCritical());
            smpPlayer.getActiveEnchants().forEach((enchants, integer) -> {
                enchants.getEnchant().onDamageEntity(damageContext, integer, smpPlayer);
            });

            smpPlayer.getActiveAttributes().forEach((attributes, aDouble) -> {
                attributes.getAttribute().onDamageEntity(damageContext, aDouble, smpPlayer);
            });
            event.setCancelled(damageContext.isCancelled());
            event.setDamage(damageContext.calculateFinalDamage());
            event.getDamager().sendMessage(String.valueOf(event.getDamage()));
            return;
        }

        //Disallow pvp
        event.setCancelled(true);

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
        smpPlayer.getActiveEnchants().forEach((enchants, integer) -> {
            enchants.getEnchant().onProjectileHit(event, integer, smpPlayer);
        });
        smpPlayer.getActiveAttributes().forEach((attributes, aDouble) -> {
            attributes.getAttribute().onProjectileHit(event, aDouble, smpPlayer);
        });
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        SmpPlayer smpPlayer = playerManager.getSmpPlayer(player.getUniqueId());
        if (smpPlayer == null) return;
        smpPlayer.getActiveEnchants().forEach((enchants, integer) -> {
            enchants.getEnchant().onProjectileLaunch(event, integer, smpPlayer);
        });
        smpPlayer.getActiveAttributes().forEach((attributes, aDouble) -> {
            attributes.getAttribute().onProjectileLaunch(event, aDouble, smpPlayer);
        });
    }
}
