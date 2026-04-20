package com.roguesmp.dungeon_v2.controller_;

import com.roguesmp.dungeon_v2.data.definition.room.Room;
import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.service.impl.DungeonLifecycleService;
import com.roguesmp.dungeon_v2.service.impl.DungeonTimerService;
import com.roguesmp.dungeon_v2.service.impl.RoomNavigationService;
import com.roguesmp.dungeon_v2.service.impl.TreasureService;
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
    public void handleSelectNextRoom(Player player, Block door, Room room) {
        navigationService.handleSelectNextRoom(player, door, room);
    }
    public void handleOpenTreasurePortal(Block door) {
        treasureService.handleOpenTreasurePortal(door);
    }
    public void handleGetIntoTreasurePortal(Player player) {
        treasureService.handleGetIntoTreasurePortal(player);
    }
    public void handleLeaveDungeon(Player player) {
        lifecycleService.onLeaveDungeon(player);
    }
    public void handleEndUpDungeon(DungeonInstance instance) {
        lifecycleService.onEndUpDungeon(instance);
    }
    public void handleDungeonTimerTick() {
        timerService.handleDungeonTimerTick();
    }
    public void handleDungeonTimeExpired(DungeonInstance instance) {
        lifecycleService.onDungeonTimeExpired(instance);
    }
    public void handleDungeonLose(DungeonInstance instance) {
        lifecycleService.onDungeonFinish(instance);
    }
}
