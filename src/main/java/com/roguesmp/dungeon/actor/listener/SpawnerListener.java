package com.roguesmp.dungeon.actor.listener;

import com.roguesmp.dungeon.controller.SpawnerController;
import com.roguesmp.dungeon.controller.response.ControllerResponse;
import com.roguesmp.dungeon.ultis.ConsoleLogger;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
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
    public void onMarkerLoad(EntitiesLoadEvent event){
        spawnerController.spawnerMarkerLoad(event);
    }

    @EventHandler
    public void onSpawnerSpawn(SpawnerSpawnEvent event){
        if(event.getLocation().getWorld().getName().contains("dungeon_")) return;
        CreatureSpawner spawner = event.getSpawner();
        if (spawner == null) return;

        Block block = spawner.getBlock();
        //check psd id
        //get entity spawner want to spawn
        //copy then remove it, and run for loop with random location to spawn entities as setting

    }

    @EventHandler
    public void onSpawnerBreak(BlockBreakEvent event){
        // cancel event if on condition
    }
}
