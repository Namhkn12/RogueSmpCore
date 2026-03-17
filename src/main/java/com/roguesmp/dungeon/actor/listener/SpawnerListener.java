package com.roguesmp.dungeon.actor.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Marker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.world.EntitiesLoadEvent;

public class SpawnerListener implements Listener {

    @EventHandler
    public void onSpawnerPlace(BlockPlaceEvent event){
        if(event.getBlock().getType() == Material.SPAWNER){
            Block spawner = event.getBlock();
            World world = spawner.getWorld();
            // check block có psd ko - lấy ra
            // đặt 1 entity marker lên trên
            Location loc = spawner.getLocation().add(0.5, 0, 0.5);

            Marker marker = world.spawn(loc, Marker.class, entity -> {
                entity.setPersistent(true);
                entity.setInvulnerable(true);
            });

        }
    }

    @EventHandler
    public void onMarkerLoad(EntitiesLoadEvent event){
        if(event.getWorld().getName().contains("dungeon_")) return;
        for (Entity e : event.getEntities()){
            if(e.getType() == EntityType.MARKER){
                //check psd id
                //load data into block below
            }
        }

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
