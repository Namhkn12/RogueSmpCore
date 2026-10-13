package com.roguesmp.dungeon.controller;

import com.roguesmp.dungeon.service.DungeonRewardService;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class DungeonTreasureController {

    private final DungeonRewardService rewardService;

    public DungeonTreasureController(DungeonRewardService rewardService) {
        this.rewardService = rewardService;
    }

    public void handlePlaceLootChest(BlockPlaceEvent e){
        if(!rewardService.onPlace(e)){
            return;
        }
        return;
    }

    public void handleBreakLootChest(BlockBreakEvent e){
        if(!rewardService.onBreak(e)){
            return;
        }
        return;
    }

    public void handleOpenLootChestInDungeon(PlayerInteractEvent e){
        if(!rewardService.onOpen(e)){
            return;
        }
        return;
    }

}
