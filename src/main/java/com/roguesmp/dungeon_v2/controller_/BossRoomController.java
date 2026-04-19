package com.roguesmp.dungeon_v2.controller_;

import com.roguesmp.dungeon_v2.data.definition.objective.impl.DemonSlayer;
import com.roguesmp.dungeon_v2.data.definition.room.Room;
import com.roguesmp.dungeon_v2.data.definition.room.RoomType;
import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.data.runtime.Party;
import com.roguesmp.dungeon_v2.manager.InstanceManager;
import com.roguesmp.dungeon_v2.manager.RoomManager;
import com.roguesmp.dungeon_v2.service.IPartyService;
import com.roguesmp.registry.entity.EntityRegistry;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class BossRoomController {

    private final InstanceManager instanceManager;
    private final IPartyService partyService;
    private final RoomManager roomManager;

    public BossRoomController(InstanceManager instanceManager, IPartyService partyService, RoomManager roomManager) {
        this.instanceManager = instanceManager;
        this.partyService = partyService;
        this.roomManager = roomManager;
    }

    public void handleOpenBossRoom(Player player, Block spawn){
        Party party = partyService.getPartyByPlayer(player);
        if(party == null || party.getInstanceId() == null || party.getInstanceId().isBlank()) return;
        DungeonInstance instance = instanceManager.get(party.getInstanceId());
        if (instance == null) return;
        /*We treat the boss room as a normal objective room with different active method*/
        String rid = instance.getProgress().getCurrentRoom().getRoomId();
        Room currentR = roomManager.get(rid);
        if(currentR.getType() != RoomType.BOSS) return;
        /*Try to get objective data from the Boss Room*/
        String bossId = currentR.getObjectives().stream()
                        .filter(cfg -> DemonSlayer.TYPE.equals(cfg.getType()))
                                .map(cfg -> cfg.getParams().get("target"))
                                        .filter(v -> v instanceof String)
                                                .map(v -> (String) v)
                                                        .findFirst()
                                                                .orElse("fallback here");
        /*Spawn boss*/
        /*Todo: Should have a manager for spawn boss effect */
        EntityRegistry.getInstance().spawnEntity(bossId, spawn.getLocation().add(0, 3, 0));
    }

    public void handleTriggerBossRoom(Player player, Block trigger){
        Party party = partyService.getPartyByPlayer(player);
        if(party == null || party.getInstanceId() == null || party.getInstanceId().isBlank()) return;
        DungeonInstance instance = instanceManager.get(party.getInstanceId());
        if (instance == null) return;
        /*We treat the boss room as a normal objective room with different active method*/
        String rid = instance.getProgress().getCurrentRoom().getRoomId();
        Room currentR = roomManager.get(rid);
        if(currentR.getType() != RoomType.BOSS) return;
        /*Try to get objective data from the Boss Room*/
        String bossId = currentR.getObjectives().stream()
                .filter(cfg -> DemonSlayer.TYPE.equals(cfg.getType()))
                .map(cfg -> cfg.getParams().get("target"))
                .filter(v -> v instanceof String)
                .map(v -> (String) v)
                .findFirst()
                .orElse("fallback here");
        /*Call spawn boss here*/
        EntityRegistry.getInstance().spawnEntity(bossId, trigger.getLocation().add(0.5 , 3, 0.5));
    }
}
