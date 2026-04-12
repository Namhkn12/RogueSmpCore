package com.roguesmp.dungeon.controller;

import com.roguesmp.dungeon.actor.scoreboard.MemberStatus;
import com.roguesmp.dungeon.dto.ActionResult;
import com.roguesmp.dungeon.data.Dungeon;
import com.roguesmp.dungeon.data.Party;
import com.roguesmp.dungeon.data.Region;
import com.roguesmp.dungeon.data.Schemeta;
import com.roguesmp.dungeon.dto.DungeonScoreBoard;
import com.roguesmp.dungeon.dto.NextRoom;
import com.roguesmp.dungeon.exception.BaseException;
import com.roguesmp.dungeon.exception.GlobalException;
import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.instance.NodeInstance;
import com.roguesmp.dungeon.instance.RegionInstance;
import com.roguesmp.dungeon.instance.RoomInstance;
import com.roguesmp.dungeon.manager.ScoreBoardManager;
import com.roguesmp.dungeon.objective_.IObjective;
import com.roguesmp.dungeon.objective_.param.ObjectiveData;
import com.roguesmp.dungeon.objective_.ObjectiveFactory;
import com.roguesmp.dungeon.presentation.presenter.DungeonPresenter;
import com.roguesmp.dungeon.service.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class DungeonController {

    private final IInstanceService instanceService;
    private final IDungeonFlowService dungeonFlowService;

    public DungeonController(IInstanceService instanceService, IDungeonFlowService dungeonFlowService) {
        this.instanceService = instanceService;
        this.dungeonFlowService = dungeonFlowService;
    }

    public ActionResult<Void> handleRequestDungeon(String dungeonId, Player player){
        DungeonInstance instance = dungeonFlowService.onGenerateDungeon(dungeonId, player);
        if(instance == null){
            //fallback - end dungeon
            return ActionResult.failed("Dungeon instance is null! Generate failed");
        }
        boolean result = dungeonFlowService.onStartDungeon(instance);
        if(!result) return ActionResult.failed("Cannot generate and start the dungeon");
        return ActionResult.ok("Dungeon with id " + instance.getUuid() + " request by " + player.getName() + " started");
    }

    public ActionResult<List<NextRoom>> getNextRooms(DungeonInstance instance) {
        List<NextRoom>  nextRooms = dungeonFlowService.onOpenDoorGui(instance);
        return ActionResult.ok(nextRooms);
    }

    public ActionResult<RoomInstance> selectNextRoom(DungeonInstance dungeonInstance, NodeInstance selectNode) {
        RoomInstance roomInstance = dungeonFlowService.onOpenNextRoom(dungeonInstance, selectNode);
        return ActionResult.ok(roomInstance);
    }

    public ActionResult<DungeonInstance> getInstanceByParty(UUID partyId){
        DungeonInstance instance = instanceService.getInstance(partyId).orElse(null);
        return ActionResult.ok("Got instance of party successfully", instance);
    }

}
