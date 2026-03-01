package com.roguesmp.dungeon;

import com.roguesmp.dungeon.instance.DungeonInstanceManager;
import com.roguesmp.dungeon.party.PartyManager;
import com.roguesmp.dungeon.region.RegionManager;
import com.roguesmp.dungeon.room.RoomManager;
import com.roguesmp.dungeon.schemeta.SchemetaManager;

import java.util.HashMap;
import java.util.Map;

public class DungeonController {
    // key: partyId, value: dungeonInstanceId
    private final Map<String, String> dungeon = new HashMap<>();

    private final DungeonInstanceManager dungeonInstanceManager;
    private final RoomManager roomManager;
    private final RegionManager regionManager;
    private PartyManager partyManager;
    private SchemetaManager schemetaManager;
    private DungeonManager dungeonManager;

    public DungeonController(DungeonInstanceManager dungeonInstanceManager, RoomManager roomManager, RegionManager regionManager, PartyManager partyManager, SchemetaManager schemetaManager, DungeonManager dungeonManager) {
        this.dungeonInstanceManager = dungeonInstanceManager;
        this.roomManager = roomManager;
        this.regionManager = regionManager;
        this.partyManager = partyManager;
        this.schemetaManager = schemetaManager;
        this.dungeonManager = dungeonManager;
    }

    //find dungeon with party id
    public void startDungeonInstance(String partyId, String dungeonId){
        regionManager
        dungeonInstanceManager.createDungeonInstance(partyId, )
    }

    //create dungeon with party

    //remove playing dungeon



}
