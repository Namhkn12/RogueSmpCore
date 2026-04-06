package com.roguesmp.dungeon_v2.controller_;

import com.roguesmp.dungeon_v2.data.definition.Dungeon;
import com.roguesmp.dungeon_v2.data.definition.room.Room;
import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.data.runtime.Party;
import com.roguesmp.dungeon_v2.data.runtime.Region;
import com.roguesmp.dungeon_v2.manager.DungeonManager;
import com.roguesmp.dungeon_v2.manager.InstanceManager;
import com.roguesmp.dungeon_v2.manager.RoomManager;
import com.roguesmp.dungeon_v2.manager.ScoreBoardManager;
import com.roguesmp.dungeon_v2.presentation.presenter.DungeonPresenter;
import com.roguesmp.dungeon_v2.service.*;
import com.roguesmp.dungeon_v2.utils.Teleporter;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class DungeonFlowController {

    private final IInstanceService instanceService;
    private final InstanceManager instanceManager;
    private final IPartyService partyService;
    private final IRegionService regionService;
    private final ScoreBoardManager scoreBoardManager;
    private final DungeonManager dungeonManager;
    private final RoomManager roomManager;
    private final DungeonPresenter dungeonPresenter;

    public DungeonFlowController(IInstanceService instanceService, InstanceManager instanceManager, IPartyService partyService,
                                 IRegionService regionService, ScoreBoardManager scoreBoardManager,
                                 DungeonManager dungeonManager, RoomManager roomManager, DungeonPresenter dungeonPresenter) {
        this.instanceService = instanceService;
        this.instanceManager = instanceManager;
        this.partyService = partyService;
        this.regionService = regionService;
        this.scoreBoardManager = scoreBoardManager;
        this.dungeonManager = dungeonManager;
        this.roomManager = roomManager;
        this.dungeonPresenter = dungeonPresenter;
    }

    void handleStartDungeon(String did, Player player){
        /*Prepare data*/
        Party party = partyService.getPartyByPlayer(player);
        if(party == null){
            return;
        }
        Dungeon dungeon = dungeonManager.get(did);
        /*Start dungeon*/
        DungeonInstance instance = instanceService.createDungeonInstance(dungeon, party);
        /*Start instance*/
        instanceService.startDungeonInstance(instance);
        Region region = regionService.getRegionById(instance.getSession().getRegionId());
        /*Teleport player to spawn room (region point)*/
        Teleporter.teleportAllByID(party.getMembers(), region.getRegionPoint());
        /*Presentation*/
        partyService.getOnlineMembers(party).forEach(p -> {
            /*Build scoreboard*/
            scoreBoardManager.createBoard(p, instance, dungeon);
            /*Build Effect and Sound*/
            dungeonPresenter.onEnterDungeon(p, dungeon.getName());
        });
    }

    void handleOpenNextDoor(Block door, Player player){
        /*Get instance by player*/
        Party party = partyService.getPartyByPlayer(player);
        DungeonInstance instance = instanceManager.get(party.getInstanceId());
        /*Get next rooms*/
        List<Room> nextRooms = instance.getProgress()
                .getNextRooms()
                .stream()
                .map(roomManager::get)
                .toList();

        /*Open next room select ui for player*/
        //when open update door logic and remove when close ui
    }

    void handleSelectNextRoom(DungeonInstance instance, Block door, String rid){
        //this api will be call in UI
        //service do : in progress of instance
        //service do: paste room
    }

    void handleEndUpDungeon(DungeonInstance instance){
        //update state of dungeon , when player left all, remove instance
    }
}
