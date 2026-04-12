package com.roguesmp.dungeon_v2.actor.listener;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.roguesmp.dungeon_v2.controller_.BossRoomController;
import com.roguesmp.dungeon_v2.controller_.SpawnerEventController;
import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.utils.NameSpaceKeys;
import com.roguesmp.dungeon_v2.utils.PdcUtil;
import com.roguesmp.dungeon_v2.utils.filterchain.FilterChain;
import com.roguesmp.dungeon_v2.utils.filterchain.impl.BlockBreakFilters;
import com.roguesmp.dungeon_v2.utils.filterchain.impl.BlockPlaceFilters;
import com.roguesmp.dungeon_v2.utils.filterchain.impl.EntityFilters;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.entity.TrialSpawnerSpawnEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
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
        boolean passes = FilterChain.of(BlockPlaceEvent.class)
                .require(BlockPlaceFilters.blockType(Material.SPAWNER))
                .require(BlockPlaceFilters.hasMeta())
                .require(BlockPlaceFilters.hasPdc(NameSpaceKeys.SPAWNER_TID_KEY, event.getItemInHand().getItemMeta()))
                .build().test(event);
        if (!passes) return;

        String sid = PdcUtil.getOrDefault(
                event.getItemInHand(),
                NameSpaceKeys.SPAWNER_TID_KEY,
                PersistentDataType.STRING,
                "unknown"
        );

        spawnerController.handlePlaceSpawner(event.getBlock(), sid);
    }

    @EventHandler
    public void onSpawnerAddToWorld(EntityAddToWorldEvent event){
        boolean filter = FilterChain.of(EntityAddToWorldEvent.class)
                .require(EntityFilters.entityType(Marker.class))
                .build()
                .test(event);
        if(!filter) return;
        Entity entity = event.getEntity();

        spawnerController.handleSpawnerAppear((Marker) entity);
    }

    @EventHandler
    public void onMarkerLoad(EntitiesLoadEvent event){
        if (!event.getWorld().getName().startsWith("dungeon_")) return;
        spawnerController.handleSpawnerLoad(event.getEntities(), event.getWorld().getName());
    }

    @EventHandler
    public void onSpawnerBreak(BlockBreakEvent event){
        boolean filter = FilterChain.of(BlockBreakEvent.class)
                .require(BlockBreakFilters.blockType(Material.SPAWNER))
                .require(BlockBreakFilters.inWorld("dungeon_"))
                .build()
                .test(event);
        if(!filter) return;
        boolean allowBreak  = spawnerController.handleSpawnerBreak(event.getBlock(), event.getPlayer());
        event.setCancelled(!allowBreak );
    }

    @EventHandler
    public void onSpawnerSpawn(SpawnerSpawnEvent event){
    }

    @EventHandler
    public void onTrialSpawnerActive(TrialSpawnerSpawnEvent event){
        if(!event.getEntity().getWorld().getName().startsWith("dungeon_")) return;
        event.setCancelled(true);
        Player nearly = event.getTrialSpawner().getTrackedPlayers().stream().findFirst().get();
        bossRoomController.handleOpenBossRoom(nearly, event.getTrialSpawner().getBlock());
        event.getTrialSpawner().getBlock().setType(Material.BEACON);
    }

}
