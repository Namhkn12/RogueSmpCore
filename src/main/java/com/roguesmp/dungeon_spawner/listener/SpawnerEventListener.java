package com.roguesmp.dungeon_spawner.listener;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.block.Action;
import com.roguesmp.constant.Keys;
import com.roguesmp.world.DungeonWorldManager;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.Component;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.roguesmp.dungeon.actor.ui.TriggerBossGui;
import com.roguesmp.dungeon.controller.BossRoomController;
import com.roguesmp.dungeon_spawner.controller.SpawnerEventController;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TrialSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;

public class SpawnerEventListener implements Listener {

    private final SpawnerEventController spawnerController;
    private final BossRoomController bossRoomController;

    public SpawnerEventListener(SpawnerEventController spawnerController, BossRoomController bossRoomController) {
        this.spawnerController = spawnerController;
        this.bossRoomController = bossRoomController;
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

    @EventHandler
    public void onTrialSpawnerActive(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND || block == null) return;
        if (!(block.getState() instanceof TrialSpawner) || !DungeonWorldManager.isAnyDungeonWorld(block.getWorld())) return;

        new TriggerBossGui(block, p -> bossRoomController.handleTriggerBossRoom(p, block)).showInventory(event.getPlayer());
    }


}
