package com.roguesmp.dungeon.controller;

import com.roguesmp.dungeon.dto.ActionResult;
import com.roguesmp.dungeon.service.IDungeonRewardService;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class DungeonTreasureController {

    private final IDungeonRewardService rewardService;

    public DungeonTreasureController(IDungeonRewardService rewardService) {
        this.rewardService = rewardService;
    }

    public ActionResult<Void> handlePlaceLootChest(BlockPlaceEvent e){
        if(!rewardService.onPlace(e)){
            return ActionResult.ok("Failed place a dungeon reward chest");
        }
        return ActionResult.ok("Successfully place a dungeon reward chest");
    }

    public ActionResult<Void> handleBreakLootChest(BlockBreakEvent e){
        if(!rewardService.onBreak(e)){
            return ActionResult.ok("Dungeon reward chest now able to break");
        }
        return ActionResult.ok("Successfully cancel break a dungeon reward chest");
    }

    public ActionResult<Void> handleOpenLootChestInDungeon(PlayerInteractEvent e){
        if(!rewardService.onOpen(e)){
            return ActionResult.ok("This reward chest can not be opened");
        }
        return ActionResult.ok("Successfully open the dungeon reward chest");
    }

}
