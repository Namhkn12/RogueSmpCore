package com.roguesmp.dungeon.actor.listener;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.roguesmp.dungeon.controller.SpawnerController;
import com.roguesmp.dungeon.dto.ActionResult;
import com.roguesmp.dungeon.utils.NameSpaceKeys;
import com.roguesmp.dungeon.utils.filterchain.FilterChain;
import com.roguesmp.dungeon.utils.filterchain.impl.BlockPlaceFilters;
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

        String templateId = event.getItemInHand()
                .getItemMeta()
                .getPersistentDataContainer()
                .get(NameSpaceKeys.SPAWNER_TID_KEY, PersistentDataType.STRING);

        spawnerController.handleSpawnerPlace(event.getBlock(), templateId);
    }

    @EventHandler
    public void onSpawnerAddToWorld(EntityAddToWorldEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Marker marker)) return;

        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (!pdc.has(NameSpaceKeys.SPAWNER_TID_KEY, PersistentDataType.STRING)) return;

        String templateId = pdc.get(NameSpaceKeys.SPAWNER_TID_KEY, PersistentDataType.STRING);
        spawnerController.handleSpawnerAppear(marker, templateId);
    }

    @EventHandler
    public void onMarkerLoad(EntitiesLoadEvent event) {
        if (!event.getWorld().getName().startsWith("dungeon_")) return;
        spawnerController.handleMarkerLoad(event.getEntities(), event.getWorld().getName());
    }

    @EventHandler
    public void onSpawnerBreak(BlockBreakEvent event) {
        if (!event.getPlayer().getWorld().getName().startsWith("dungeon_")) return;
        if (event.getBlock().getType() != Material.SPAWNER) return;
        spawnerController.handleSpawnerBreak(event.getBlock(), event);
    }
}
