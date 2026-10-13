package com.roguesmp.dungeon.controller;

import com.roguesmp.dungeon.service.DungeonLifecycleService;
import com.roguesmp.dungeon.service.DungeonTimerService;
import com.roguesmp.dungeon.service.RoomNavigationService;
import com.roguesmp.dungeon.service.TreasureService;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class DungeonFlowController {

    private final DungeonLifecycleService lifecycleService;
    private final RoomNavigationService navigationService;
    private final TreasureService treasureService;
    private final DungeonTimerService timerService;

    public DungeonFlowController(DungeonLifecycleService lifecycleService, RoomNavigationService navigationService, TreasureService treasureService, DungeonTimerService timerService) {
        this.lifecycleService = lifecycleService;
        this.navigationService = navigationService;
        this.treasureService = treasureService;
        this.timerService = timerService;
    }

    public void handleStartDungeon(String did, Player player) {
        lifecycleService.onStartDungeon(did, player);
    }
    public void handleOpenNextDoor(Block door, Player player) {
        navigationService.handleOpenNextDoor(door, player);
    }
    public void handleGetIntoTreasurePortal(Player player) {
        treasureService.handleGetIntoTreasurePortal(player);
    }
    public void handleLeaveDungeon(Player player) {
        lifecycleService.onLeaveDungeon(player);
    }
    public void handleDungeonTimerTick() {
        timerService.handleDungeonTimerTick();
    }
}
