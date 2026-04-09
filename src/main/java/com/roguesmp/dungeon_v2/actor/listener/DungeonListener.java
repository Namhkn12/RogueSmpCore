package com.roguesmp.dungeon_v2.actor.listener;

import com.roguesmp.dungeon_v2.controller_.PlayerActionController;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;

public class DungeonListener implements Listener {

    private final PlayerActionController actionController;

    public DungeonListener(PlayerActionController actionController) {
        this.actionController = actionController;
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

    //TODO

}
