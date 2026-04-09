package com.roguesmp.dungeon_v2.actor.listener;

import com.roguesmp.dungeon_v2.controller_.DungeonTreasureController;
import com.roguesmp.dungeon_v2.utils.NameSpaceKeys;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;

public class LootTableListener implements Listener {

    private final DungeonTreasureController treasureController;

    public LootTableListener(DungeonTreasureController treasureController) {
        this.treasureController = treasureController;
    }

    @EventHandler
    public void onLootChestPlace(BlockPlaceEvent e){
        treasureController.handlePlaceLootChest(e);
    }

    @EventHandler
    public void onLootChestBreak(BlockBreakEvent e){
        treasureController.handleBreakLootChest(e);
    }

    @EventHandler
    public void onLootChestOpen(PlayerInteractEvent e){
        treasureController.handleOpenLootChestInDungeon(e);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> {
            if (!(block.getState() instanceof TileState ts)) return false;
            return ts.getPersistentDataContainer().has(NameSpaceKeys.REWARD_CID_KEY, PersistentDataType.STRING);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> {
            if (!(block.getState() instanceof TileState ts)) return false;
            return ts.getPersistentDataContainer().has(NameSpaceKeys.REWARD_CID_KEY, PersistentDataType.STRING);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (!(block.getState() instanceof TileState ts)) continue;
            if (ts.getPersistentDataContainer().has(NameSpaceKeys.REWARD_CID_KEY, PersistentDataType.STRING)) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
