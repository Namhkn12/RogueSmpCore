package com.roguesmp.dungeon_v2.actor.listener;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.roguesmp.dungeon_v2.actor.ui.TriggerBossGui;
import com.roguesmp.dungeon_v2.controller_.BossRoomController;
import com.roguesmp.dungeon_v2.controller_.SpawnerEventController;
import com.roguesmp.dungeon_v2.utils.DungeonEcho;
import com.roguesmp.dungeon_v2.utils.NameSpaceKeys;
import com.roguesmp.dungeon_v2.utils.PdcUtil;
import com.roguesmp.dungeon_v2.utils.filterchain.EventFilter;
import com.roguesmp.dungeon_v2.utils.filterchain.FilterChain;
import com.roguesmp.dungeon_v2.utils.filterchain.impl.BlockBreakFilters;
import com.roguesmp.dungeon_v2.utils.filterchain.impl.BlockPlaceFilters;
import com.roguesmp.dungeon_v2.utils.filterchain.impl.EntityFilters;
import com.roguesmp.dungeon_v2.utils.filterchain.impl.InteractFilters;
import org.bukkit.Material;
import org.bukkit.block.Block;
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
        DungeonEcho.info(event.getPlayer(), "Place spawner with sid: " + sid);
        spawnerController.handlePlaceSpawner(event.getBlock(), sid);
    }

    @EventHandler
    public void onSpawnerAddToWorld(EntityAddToWorldEvent event){
        boolean filter = FilterChain.of(EntityAddToWorldEvent.class)
                .require(EntityFilters.entityType(Marker.class))
                .build()
                .test(event);
        if(!filter) return;
        if(!event.getWorld().getName().startsWith("dungeon_")) return;
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
    public void onTrialSpawnerActive(PlayerInteractEvent event){
        if(!TRIAL_FILTER.test(event)) return;
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        new TriggerBossGui(p -> {
            bossRoomController.handleTriggerBossRoom(player, block);
        }).showInventory(player);
    }

    private static final EventFilter<PlayerInteractEvent> TRIAL_FILTER =
            FilterChain.of(PlayerInteractEvent.class)
                    .require(InteractFilters.rightClickBlock())
                    .require(InteractFilters.mainHand())
                    .require(InteractFilters.hasBlock())
                    .require(InteractFilters.blockState(TrialSpawner.class))
                    .require(InteractFilters.clickedBlockInWorld("dungeon_"))
                    .build();

}
