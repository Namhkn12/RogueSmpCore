package com.roguesmp.dungeon_v2.controller_;

import com.roguesmp.dungeon_v2.actor.ui.OpenDoorGui;
import com.roguesmp.dungeon_v2.data.definition.Dungeon;
import com.roguesmp.dungeon_v2.data.definition.objective.BaseObjective;
import com.roguesmp.dungeon_v2.data.definition.objective.IObjective;
import com.roguesmp.dungeon_v2.data.definition.objective.factory.ObjectiveConfig;
import com.roguesmp.dungeon_v2.data.definition.objective.factory.ObjectiveFactory;
import com.roguesmp.dungeon_v2.data.definition.room.Room;
import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.data.runtime.Party;
import com.roguesmp.dungeon_v2.data.runtime.Region;
import com.roguesmp.dungeon_v2.data.runtime.RoomInstance;
import com.roguesmp.dungeon_v2.data.runtime.session.DungeonProgress;
import com.roguesmp.dungeon_v2.helper.SerializableBounds;
import com.roguesmp.dungeon_v2.helper.SerializableLocation;
import com.roguesmp.dungeon_v2.manager.DungeonManager;
import com.roguesmp.dungeon_v2.manager.InstanceManager;
import com.roguesmp.dungeon_v2.manager.RoomManager;
import com.roguesmp.dungeon_v2.manager.ScoreBoardManager;
import com.roguesmp.dungeon_v2.presentation.presenter.DungeonPresenter;
import com.roguesmp.dungeon_v2.service.*;
import com.roguesmp.dungeon_v2.utils.DungeonEcho;
import com.roguesmp.dungeon_v2.utils.Teleporter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
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
    private final IRoomService roomService;
    private final DungeonPresenter dungeonPresenter;
    private final ISchematicService schematicService;
    private final IDungeonService dungeonService;

    public DungeonFlowController(IInstanceService instanceService, InstanceManager instanceManager, IPartyService partyService,
                                 IRegionService regionService, ScoreBoardManager scoreBoardManager,
                                 DungeonManager dungeonManager, RoomManager roomManager, IRoomService roomService, DungeonPresenter dungeonPresenter, ISchematicService schematicService, IDungeonService dungeonService) {
        this.instanceService = instanceService;
        this.instanceManager = instanceManager;
        this.partyService = partyService;
        this.regionService = regionService;
        this.scoreBoardManager = scoreBoardManager;
        this.dungeonManager = dungeonManager;
        this.roomManager = roomManager;
        this.roomService = roomService;
        this.dungeonPresenter = dungeonPresenter;
        this.schematicService = schematicService;
        this.dungeonService = dungeonService;
    }

    public void handleStartDungeon(String did, Player player){
        /*Prepare data*/
        Party party = partyService.getPartyByPlayer(player);
        if(party == null){
            return;
        }
        Dungeon dungeon = dungeonManager.get(did);
        /*Start dungeon*/
        DungeonInstance instance = instanceService.createDungeonInstance(dungeon, party);
        party.setInstanceId(instance.getSession().getSessionId());
        instanceManager.add(instance);
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

    public void handleOpenNextDoor(Block door, Player player){
        Party party = partyService.getPartyByPlayer(player);
        DungeonInstance instance = instanceManager.get(party.getInstanceId());
        DungeonProgress progress = instance.getProgress();

        /*Boss defeated → open treasure portal*/
        if(progress.getStatus() == DungeonProgress.Status.BOSS_DEFEATED){
            handleOpenTreasurePortal(door);
            return;
        }

        /*Check current room completed*/
        if(!progress.getCurrentRoom().isCompleted()) return;

        /*Roll next rooms nếu chưa có (tránh roll lại khi click nhiều lần)*/
        if(progress.getNextRooms() == null || progress.getNextRooms().isEmpty()){
            List<String> nextRoomIds = dungeonService.rollNextRoomFromPool(
                    progress.getRoomPool(),
                    progress.getMinimumRooms(),
                    progress.getClearedRooms()
            );
            progress.setNextRooms(nextRoomIds);
            instanceManager.add(instance);
        }

        List<Room> nextRooms = progress.getNextRooms()
                .stream()
                .map(roomManager::get)
                .toList();

        new OpenDoorGui(door, nextRooms, this::handleSelectNextRoom).showInventory(player);
    }

    public void handleSelectNextRoom(Player player, Block door, Room room){
        Party party = partyService.getPartyByPlayer(player);
        DungeonInstance instance = instanceManager.get(party.getInstanceId());
        DungeonProgress progress = instance.getProgress();
        Location doorLoc = door.getLocation();
        Location pasteLoc = doorLoc.clone().add(0, -2, 0);

        /*Build selected room*/
        BoundingBox area = schematicService.paste(room.getSchemetaId(), pasteLoc);
        RoomInstance roomInstance = new RoomInstance(room.getId(), SerializableBounds.from(area));
        /*Prepare objective and set door location*/
        List<IObjective> objs = new ArrayList<>();
        for(ObjectiveConfig objConfig : room.getObjectives()){
            IObjective obj = ObjectiveFactory.create(objConfig);
            if(obj instanceof BaseObjective base){
                base.setCallback(objective -> checkRoomCompletion(roomInstance, objs, party));
            }
            objs.add(obj);
        }
        roomInstance.setActiveObjectives(objs);
        /*If room doesn't have a goal, mark it clear*/
        roomInstance.setCompleted(objs.isEmpty());
        roomInstance.setDoor(SerializableLocation.from(doorLoc));

        /*Mark current room cleared, set new room*/
        progress.markCurrentRoomCleared();
        progress.setCurrentRoom(roomInstance);
        progress.setNextRooms(null);

        /*Open the door*/
        roomService.openRoomDoor(doorLoc);
    }

    public void handleOpenTreasurePortal(Block door){
        Location doorLoc = door.getLocation();
        World world = doorLoc.getWorld();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                world.getBlockAt(doorLoc.clone().add(dx, dy, 0)).setType(Material.NETHER_PORTAL);
            }
        }
    }

    public void handleEndUpDungeon(DungeonInstance instance){
        //TODO
        //update state of dungeon , when player left all, remove instance

        //end by time

        //end by player


    }

    private void checkRoomCompletion(RoomInstance room, List<IObjective> objectives, Party party) {
        boolean allDone = objectives.stream()
                .filter(o -> o instanceof BaseObjective)
                .map(o -> (BaseObjective) o)
                .allMatch(BaseObjective::isCompleted);

        if (allDone) {
            room.setCompleted(true);
            // trigger mở cửa, next room, v.v.
            DungeonEcho.success(Bukkit.getPlayer(party.getOwner()), "Hoan thanh nhien vu");
        }
    }
}
