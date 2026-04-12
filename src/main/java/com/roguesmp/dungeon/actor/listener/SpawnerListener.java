package com.roguesmp.dungeon.actor.listener;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.roguesmp.dungeon.controller.SpawnerController;
import com.roguesmp.dungeon.dto.ActionResult;
import com.roguesmp.dungeon.utils.NameSpaceKeys;
import com.roguesmp.dungeon.utils.PdcUtil;
import com.roguesmp.dungeon.utils.filterchain.FilterChain;
import com.roguesmp.dungeon.utils.filterchain.impl.BlockBreakFilters;
import com.roguesmp.dungeon.utils.filterchain.impl.BlockPlaceFilters;
import com.roguesmp.dungeon.utils.filterchain.impl.EntityFilters;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Marker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public class SpawnerListener implements Listener {

    private final SpawnerController spawnerController;

    public SpawnerListener(SpawnerController spawnerController) {
        this.spawnerController = spawnerController;
    }

    @EventHandler
    public void onSpawnerPlace(BlockPlaceEvent event) {
        boolean passes = FilterChain.of(BlockPlaceEvent.class)
                .require(BlockPlaceFilters.blockType(Material.SPAWNER))
                .require(BlockPlaceFilters.hasMeta())
                .require(BlockPlaceFilters.hasPdc(NameSpaceKeys.SPAWNER_IID_KEY, event.getItemInHand().getItemMeta()))
                .build().test(event);
        if (!passes) return;

        String templateId = PdcUtil.getOrDefault(
                event.getItemInHand(),
                NameSpaceKeys.SPAWNER_TID_KEY,
                PersistentDataType.STRING,
                "unknown"
        );

        spawnerController.handleSpawnerPlace(event.getBlock(), templateId);
    }

    @EventHandler
    public void onSpawnerAddToWorld(EntityAddToWorldEvent event) {
        boolean filter = FilterChain.of(EntityAddToWorldEvent.class)
                .require(EntityFilters.entityType(Marker.class))
                .build()
                .test(event);
        if(!filter) return;
        Entity entity = event.getEntity();

        String templateId = PdcUtil.getOrDefault(
                entity,
                NameSpaceKeys.SPAWNER_TID_KEY,
                PersistentDataType.STRING,
                "unkown"
        );

        spawnerController.handleSpawnerAppear((Marker) entity, templateId);
    }

    @EventHandler
    public void onMarkerLoad(EntitiesLoadEvent event) {
        if (!event.getWorld().getName().startsWith("dungeon_")) return;
        spawnerController.handleMarkerLoad(event.getEntities(), event.getWorld().getName());
    }

    @EventHandler
    public void onSpawnerBreak(BlockBreakEvent event) {
        boolean filter = FilterChain.of(BlockBreakEvent.class)
                        .require(BlockBreakFilters.blockType(Material.SPAWNER))
                                .require(BlockBreakFilters.inWorld("dungeon_"))
                                        .build()
                                                .test(event);
        if(!filter) return;
        boolean shouldCancel = spawnerController.handleSpawnerBreak(event.getBlock()).getData() || false;
        event.setCancelled(shouldCancel);
    }
}
