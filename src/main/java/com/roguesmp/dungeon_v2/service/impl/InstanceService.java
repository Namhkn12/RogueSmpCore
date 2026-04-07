package com.roguesmp.dungeon_v2.service.impl;

import com.roguesmp.dungeon_v2.data.definition.Dungeon;
import com.roguesmp.dungeon_v2.data.definition.objective.IObjective;
import com.roguesmp.dungeon_v2.data.definition.objective.factory.ObjectiveConfig;
import com.roguesmp.dungeon_v2.data.definition.objective.factory.ObjectiveFactory;
import com.roguesmp.dungeon_v2.data.definition.room.Room;
import com.roguesmp.dungeon_v2.data.definition.room.RoomType;
import com.roguesmp.dungeon_v2.data.runtime.*;
import com.roguesmp.dungeon_v2.data.runtime.session.DungeonProgress;
import com.roguesmp.dungeon_v2.data.runtime.session.DungeonSession;
import com.roguesmp.dungeon_v2.data.runtime.session.DungeonTimer;
import com.roguesmp.dungeon_v2.helper.SerializableBounds;
import com.roguesmp.dungeon_v2.helper.SerializableLocation;
import com.roguesmp.dungeon_v2.manager.DungeonManager;
import com.roguesmp.dungeon_v2.manager.InstanceManager;
import com.roguesmp.dungeon_v2.manager.RoomManager;
import com.roguesmp.dungeon_v2.service.*;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InstanceService implements IInstanceService {

    private final InstanceManager instanceManager;
    private final DungeonManager dungeonManager;
    private final RoomManager roomManager;
    private final IRegionService regionService;
    private final IDungeonService dungeonService;
    private final IPartyService partyService;
    private final IRoomService roomService;
    private final ISchematicService schematicService;

    public InstanceService(InstanceManager instanceManager, DungeonManager dungeonManager, RoomManager roomManager, IRegionService regionService, IDungeonService dungeonService, IPartyService partyService, IRoomService roomService, ISchematicService schematicService) {
        this.instanceManager = instanceManager;
        this.dungeonManager = dungeonManager;
        this.roomManager = roomManager;
        this.regionService = regionService;
        this.dungeonService = dungeonService;
        this.partyService = partyService;
        this.roomService = roomService;
        this.schematicService = schematicService;
    }

    @Override
    public DungeonInstance createDungeonInstance(Dungeon dungeon, Party party) {
        /*Prepare dungeon instance*/
        DungeonInstance instance = new DungeonInstance();
        /*Try to acquire a region*/
        Region region = regionService.acquireRegion();
        /*Prepare dungeon session*/
        DungeonSession session = new DungeonSession();
        session.setSessionId(UUID.randomUUID());
        session.setRegionId(region.getId());
        session.setDungeonId(dungeon.getId());
        session.setPartyId(party.getPartyId());
        session.setStartedAt(System.currentTimeMillis());
        /*Prepare dungeon timer*/
        DungeonTimer timer = new DungeonTimer();
        timer.setStartTime(System.currentTimeMillis());
        timer.setEndTime(timer.getStartTime() + (long) dungeon.getPlayTime() *60*1000);

        /*Prepare dungeon progress*/
        DungeonProgress progress = new DungeonProgress();
        progress.setScore(0);
        progress.setCheckpoint(new SerializableLocation(region.getWorldName(),
                region.getX(), region.getY(), region.getZ(), 0, 0));
        List<String> pool = dungeonService.rollRoomPool(dungeon);
        progress.setRoomPool(pool);
        progress.setMinimumRooms(dungeon.getMinimumRooms());

        /*Set instance information*/
        instance.setSession(session);
        instance.setProgress(progress);
        instance.setTimer(timer);

        party.setInstanceId(instance.getSession().getSessionId());

        return instance;
    }

    @Override
    public void startDungeonInstance(DungeonInstance instance) {
        Party party = partyService.getPartyById(instance.getSession().getPartyId());
        Region region = regionService.getRegionById(instance.getSession().getRegionId());
        Dungeon dungeon = dungeonManager.get(instance.getSession().getDungeonId());
        /*Start rolling next rooms*/
        List<String> pool = instance.getProgress().getRoomPool();
        /*Find spawn room*/
        Room spawn = roomService.getRoomByRoomType(pool, RoomType.SPAWN);
        if(spawn == null){
            //fallback
            return;
        }
        BoundingBox area = schematicService.paste(spawn.getSchemetaId(), region.getRegionPoint());
        /*Set room instance state after build*/
        RoomInstance roomInstance = new RoomInstance(spawn.getId(), SerializableBounds.from(area));
        List<IObjective> objs = new ArrayList<>();
        for(ObjectiveConfig objConfig : spawn.getObjectives()){
            objs.add(ObjectiveFactory.create(objConfig));
        }
        roomInstance.setActiveObjectives(objs);
        roomInstance.setCompleted(objs.isEmpty());
        instance.getProgress().setCurrentRoom(roomInstance);
    }

    @Override
    public void removeDungeonInstance(DungeonInstance instance) {

    }

    @Override
    public void saveDungeonInstance(DungeonInstance instance) {

    }
}
