package com.roguesmp.dungeon.spawner;

import com.roguesmp.constant.Keys;
import com.roguesmp.dungeon.DungeonWorldManager;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.Component;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Marker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.persistence.PersistentDataType;

public class SpawnerEventListener implements Listener {

    private final SpawnerEventController spawnerController;

    public SpawnerEventListener(SpawnerEventController spawnerController) {
        this.spawnerController = spawnerController;
    }

    @EventHandler
    public void onSpawnerPlace(BlockPlaceEvent event){
        if (event.getBlock().getType() != Material.SPAWNER) return;
        String sid = event.getItemInHand().getPersistentDataContainer().get(Keys.SPAWNER_TID, PersistentDataType.STRING);
        if (sid == null) return;

        event.getPlayer().sendMessage(Component.text("Place spawner with sid: " + sid, NamedTextColor.GRAY));
        spawnerController.handlePlaceSpawner(event.getBlock(), sid);
    }

    @EventHandler
    public void onSpawnerAddToWorld(EntityAddToWorldEvent event){
        if (!(event.getEntity() instanceof Marker marker)) return;
        if (!DungeonWorldManager.isAnyDungeonWorld(event.getWorld())) return;

        spawnerController.handleSpawnerAppear(marker);
    }

//    @EventHandler
//    public void onMarkerLoad(EntitiesLoadEvent event){
//        if (!DungeonWorldManager.isAnyDungeonWorld(event.getWorld())) return;
//        spawnerController.handleSpawnerLoad(event.getEntities(), event.getWorld());
//    }

    @EventHandler
    public void onSpawnerBreak(BlockBreakEvent event){
        Block block = event.getBlock();
        if (block.getType() != Material.SPAWNER || !DungeonWorldManager.isAnyDungeonWorld(block.getWorld())) return;
        boolean allowBreak  = spawnerController.handleSpawnerBreak(event.getBlock(), event.getPlayer());
        event.setCancelled(!allowBreak );
    }

    @EventHandler
    public void onSpawnerSpawn(SpawnerSpawnEvent event){
        boolean allowSpawn = spawnerController.handleSpawnerSpawn(event.getSpawner(),
                event.getEntity(),
                event.getLocation().getNearbyPlayers(20).stream().toList());
        event.setCancelled(!allowSpawn);
    }

}
