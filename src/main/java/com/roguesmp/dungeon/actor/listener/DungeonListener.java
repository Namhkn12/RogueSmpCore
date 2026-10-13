package com.roguesmp.dungeon.actor.listener;

import com.roguesmp.constant.Keys;
import com.roguesmp.world.DungeonWorldManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import com.roguesmp.dungeon.controller.PlayerActionController;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.PortalCreateEvent;

public class DungeonListener implements Listener {

    private final PlayerActionController actionController;
    private final Plugin plugin;

    public DungeonListener(PlayerActionController actionController, Plugin plugin) {
        this.actionController = actionController;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerBreakSpawner(BlockBreakEvent event){
        if(event.isCancelled()) return;
        if(event.getBlock().getType() != Material.SPAWNER) return;
        /**/
        Player player = event.getPlayer();
        actionController.handlePlayerBreakSpawner(event.getBlock(), player);
    }

    @EventHandler
    public void onPlayerKillMobs(EntityDeathEvent event){
        if(event.isCancelled()) return;
        LivingEntity entity = event.getEntity();
        if(entity.getKiller() == null) return;
        Player player = event.getEntity().getKiller();
        if(player == null) return;
        actionController.handlePlayerKillMob(entity, player);
    }

    @EventHandler
    public void onItemCollect(EntityPickupItemEvent event){
        if(event.isCancelled()) return;
        if(!(event.getEntity() instanceof Player player)) return;
        actionController.handlePlayerCollectItem(event.getItem().getItemStack(), player);
    }

    @EventHandler
    public void onPlayerMoveInDeadMode(PlayerMoveEvent event){
        if(!DungeonWorldManager.isAnyDungeonWorld(event.getPlayer().getWorld())) return;
        Player player = event.getPlayer();
        if(player.getGameMode() != GameMode.SPECTATOR) return;

        actionController.handlePlayerMoveInDeadMode(player);
    }

    @EventHandler
    public void onPlayerReJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            actionController.handlePlayerReconnect(player);
        }, 2L);
    }

    @EventHandler
    public void onPlayerDead(PlayerDeathEvent event){
        if(!DungeonWorldManager.isAnyDungeonWorld(event.getPlayer().getWorld())) return;
        event.setCancelled(true);
        actionController.handlePlayerDead(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event){
        if(!DungeonWorldManager.isAnyDungeonWorld(event.getPlayer().getWorld())) return;
        actionController.handlePlayerDisconnect(event.getPlayer());
    }

    @EventHandler
    public void onMannequinInteract(PlayerInteractAtEntityEvent e) {
        if (e.getRightClicked() instanceof Mannequin mannequin) {
            if (mannequin.getPersistentDataContainer().has(Keys.REVIVE_POINT)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onMannequinDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Mannequin mannequin) {
            if (mannequin.getPersistentDataContainer().has(Keys.REVIVE_POINT)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEnderPearlTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        Player player = event.getPlayer();
        event.setCancelled(actionController.handlePlayerTeleport(player, event.getTo()));
    }

    @EventHandler
    public void onChorusFruitTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.CONSUMABLE_EFFECT) return;
        Player player = event.getPlayer();
        event.setCancelled(actionController.handlePlayerTeleport(player, event.getTo()));
    }

    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        if (!DungeonWorldManager.isAnyDungeonWorld(event.getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerActionInDead(PlayerInteractEvent e) {
        Player player = e.getPlayer();

        if (player.getGameMode() == GameMode.SPECTATOR) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        Player player = e.getPlayer();

        if (player.getGameMode() == GameMode.SPECTATOR) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent e) {
        Player player = e.getPlayer();

        if (player.getGameMode() == GameMode.SPECTATOR) {
            e.setCancelled(true);
        }
    }

}
