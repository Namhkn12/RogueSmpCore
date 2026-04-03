package com.roguesmp.dungeon_v2.service.impl;

import com.roguesmp.dungeon_v2.data.definition.Dungeon;
import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.data.runtime.Party;
import com.roguesmp.dungeon_v2.dto.DungeonConfig;
import com.roguesmp.dungeon_v2.manager.DungeonManager;
import com.roguesmp.dungeon_v2.manager.InstanceManager;
import com.roguesmp.dungeon_v2.manager.RoomManager;
import com.roguesmp.dungeon_v2.service.IDungeonService;
import com.roguesmp.dungeon_v2.service.IInstanceService;
import com.roguesmp.dungeon_v2.service.IPartyService;
import com.roguesmp.dungeon_v2.utils_.Log4Craft_;
import org.bukkit.entity.Player;

import java.util.List;

public class DungeonService implements IDungeonService {

    private final RoomManager roomManager;
    private final DungeonManager dungeonManager;
    private final IInstanceService instanceService;
    private final IPartyService partyService;
    private final Log4Craft_ logger;

    public DungeonService(RoomManager roomManager, DungeonManager dungeonManager, IInstanceService instanceService, IPartyService partyService, Log4Craft_ logger) {
        this.roomManager = roomManager;
        this.dungeonManager = dungeonManager;
        this.instanceService = instanceService;
        this.partyService = partyService;
        this.logger = logger;
    }

    @Override
    public DungeonInstance startDungeon(Player player, DungeonConfig config) {
        /*Get dungeon*/
        Dungeon dungeon = dungeonManager.get(config.did());
        if(dungeon == null){
            logger.error(this.getClass(), "Couldn't found dungeon with id " + config.did());
            return null;
        }
        /*Check party*/
        Party party = partyService.getPartyByPlayer(player);
        /*Create instance*/
        DungeonInstance instance = instanceService.createDungeonInstance(dungeon)
        return null;
    }

    @Override
    public List<String> rollNextRooms(DungeonInstance instance) {
        return List.of();
    }

    @Override
    public void chooseRoom(DungeonInstance instance, String chosen) {

    }
}
