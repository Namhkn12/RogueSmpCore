package com.roguesmp.dungeon.actor.listener;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.roguesmp.dungeon.controller.SpawnerController;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.world.EntitiesLoadEvent;

public class SpawnerListener implements Listener {

    private final SpawnerController spawnerController;

    public SpawnerListener(SpawnerController spawnerController) {
        this.spawnerController = spawnerController;
    }

    @EventHandler
    public void onSpawnerPlace(BlockPlaceEvent event){
        spawnerController.placeSpawnerAction(event);
    }

    @EventHandler
    public void onSpawnerAddToWorld(EntityAddToWorldEvent event){
        spawnerController.spawnerAddToWorld(event);
    }

    @EventHandler
    public void onMarkerLoad(EntitiesLoadEvent event){
        spawnerController.spawnerMarkerLoad(event);
    }

    @EventHandler
    public void onSpawnerSpawn(SpawnerSpawnEvent event){
        if(event.getLocation().getWorld().getName().contains("dungeon_")) return;

    }

    @EventHandler
    public void onSpawnerBreak(BlockBreakEvent event){
        // cancel event if on condition
        spawnerController.spawnerBreakAction(event);
    }
}
