package com.roguesmp.dungeon_v2.actor.listener;

import com.roguesmp.dungeon_v2.controller_.PlayerActionController;
import com.roguesmp.dungeon_v2.task.TaskScheduler;
import com.roguesmp.dungeon_v2.utils.NameSpaceKeys;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
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
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class DungeonListener implements Listener {

    private final PlayerActionController actionController;
    private final TaskScheduler taskScheduler;

    public DungeonListener(PlayerActionController actionController, TaskScheduler taskScheduler) {
        this.actionController = actionController;
        this.taskScheduler = taskScheduler;
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
        if(!event.getPlayer().getWorld().getName().startsWith("dungeon_")) return;
        Player player = event.getPlayer();
        if(player.getGameMode() != GameMode.SPECTATOR) return;

        actionController.handlePlayerMoveInDeadMode(player);
    }

    @EventHandler
    public void onPlayerReJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        taskScheduler.runLater(2L, () -> {
            actionController.handlePlayerReconnect(player);
        });
    }

    @EventHandler
    public void onPlayerDead(PlayerDeathEvent event){
        if(!event.getPlayer().getWorld().getName().startsWith("dungeon_")) return;
        event.setCancelled(true);
        actionController.handlePlayerDead(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event){
        if(!event.getPlayer().getWorld().getName().startsWith("dungeon_")) return;
        actionController.handlePlayerDisconnect(event.getPlayer());
    }

    @EventHandler
    public void onMannequinInteract(PlayerInteractAtEntityEvent e) {
        if (e.getRightClicked() instanceof Mannequin mannequin) {
            if (mannequin.getPersistentDataContainer().has(NameSpaceKeys.REVIVE_POINT_KEY)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onMannequinDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Mannequin mannequin) {
            if (mannequin.getPersistentDataContainer().has(NameSpaceKeys.REVIVE_POINT_KEY)) {
                e.setCancelled(true);
            }
        }
    }

    //TODO

}
