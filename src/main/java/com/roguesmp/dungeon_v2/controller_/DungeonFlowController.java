package com.roguesmp.dungeon_v2.controller_;

import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.dto.DungeonConfig;
import com.roguesmp.dungeon_v2.service.IDungeonService;
import com.roguesmp.dungeon_v2.service.IInstanceService;
import com.roguesmp.dungeon_v2.service.ISchematicService;
import com.roguesmp.dungeon_v2.service.ISchemetaService;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class DungeonFlowController {

    private final IDungeonService dungeonService;
    private final IInstanceService instanceService;
    private final ISchemetaService schemetaService;
    private final ISchematicService schematicService;

    void handleStartDungeon(String did, Player player){
        //start dungeon
        DungeonInstance instance = dungeonService.startDungeon(player, new DungeonConfig(did, 1));
        //instance start
        //after roll, paste start room
        //teleport
        //present(music, notify, effect)
        instanceService.startDungeonInstance(instance);

    }

    void handleOpenNextDoor(Block door, Player player){
        //get instance by Player

        //openUI for player
        //when open update door logic and remove when close ui
    }

    void handleSelectRoom(DungeonInstance instance, Block door, String rid){
        //this api will be call in UI
        //service do : in progress of instance
        //service do: paste room
    }

    void handleEndUpDungeon(DungeonInstance instance){
        //update state of dungeon , when player left all, remove instance
    }
}
