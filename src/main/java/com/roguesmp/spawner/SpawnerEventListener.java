package com.roguesmp.spawner;

import com.roguesmp.constant.Keys;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.PreSpawnerSpawnEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Marker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class SpawnerEventListener implements Listener {

    private final SpawnerEventController spawnerController;
    private final SpawnerVisualizer visualizer;

    public SpawnerEventListener(SpawnerEventController spawnerController, SpawnerVisualizer visualizer) {
        this.spawnerController = spawnerController;
        this.visualizer = visualizer;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        visualizer.forget(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawnerPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.SPAWNER) return;
        String sid = event.getItemInHand().getPersistentDataContainer().get(Keys.SPAWNER_TID, PersistentDataType.STRING);
        if (sid != null) spawnerController.handlePlaceSpawner(event.getBlock(), sid);
    }

    @EventHandler
    public void onSpawnerAddToWorld(EntityAddToWorldEvent event){
        if (!(event.getEntity() instanceof Marker marker)) return;
        if (!SpawnerSystem.isActive(marker.getLocation())) return;

        spawnerController.handleSpawnerAppear(marker);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawnerBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (isManagedSpawner(block) && !spawnerController.canBreak(block, event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawnerBroken(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.SPAWNER) spawnerController.handleSpawnerBroken(event.getBlock());
    }

    @EventHandler
    public void onPreSpawnerSpawn(PreSpawnerSpawnEvent event) {
        if (isManagedSpawner(event.getSpawnerLocation().getBlock())) {
            event.setCancelled(true);
            event.setShouldAbortSpawn(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(SpawnerEventListener::isManagedSpawner);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(SpawnerEventListener::isManagedSpawner);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (isManagedSpawner(event.getBlock())) event.setCancelled(true);
    }

    private static boolean isManagedSpawner(Block block) {
        if (block.getType() != Material.SPAWNER || !(block.getState(false) instanceof CreatureSpawner spawner)) return false;
        PersistentDataContainer pdc = spawner.getPersistentDataContainer();
        return pdc.has(Keys.SPAWNER_IID) || pdc.has(Keys.SPAWNER_TID);
    }

}
