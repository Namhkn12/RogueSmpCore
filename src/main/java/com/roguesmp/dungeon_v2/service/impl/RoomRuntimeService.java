package com.roguesmp.dungeon_v2.service.impl;

import com.roguesmp.dungeon_v2.data.definition.objective.BaseObjective;
import com.roguesmp.dungeon_v2.data.definition.objective.CompletionScope;
import com.roguesmp.dungeon_v2.data.definition.objective.IObjective;
import com.roguesmp.dungeon_v2.data.definition.objective.factory.ObjectiveConfig;
import com.roguesmp.dungeon_v2.data.definition.objective.factory.ObjectiveFactory;
import com.roguesmp.dungeon_v2.data.definition.room.Room;
import com.roguesmp.dungeon_v2.data.definition.room.roomevent.BaseRoomEvent;
import com.roguesmp.dungeon_v2.data.definition.room.roomevent.RoomEvent;
import com.roguesmp.dungeon_v2.data.definition.room.roomevent.RoomEventContext;
import com.roguesmp.dungeon_v2.data.definition.room.roomevent.factory.RoomEventConfig;
import com.roguesmp.dungeon_v2.data.definition.room.roomevent.factory.RoomEventFactory;
import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.data.runtime.Party;
import com.roguesmp.dungeon_v2.data.runtime.RoomInstance;
import com.roguesmp.dungeon_v2.manager.InstanceManager;
import com.roguesmp.dungeon_v2.manager.RoomManager;
import com.roguesmp.dungeon_v2.service.IPartyService;

import java.util.ArrayList;
import java.util.List;

public class RoomRuntimeService {

    private final RoomManager roomManager;
    private final InstanceManager instanceManager;
    private final IPartyService partyService;
    private RoomCompletionService completionService;

    public RoomRuntimeService(RoomManager roomManager, InstanceManager instanceManager,
                              IPartyService partyService) {
        this.roomManager = roomManager;
        this.instanceManager = instanceManager;
        this.partyService = partyService;
    }

    /** Setter injection để phá vòng tròn RoomRuntime <-> RoomCompletion */
    public void setCompletionService(RoomCompletionService completionService) {
        this.completionService = completionService;
    }

    public void bindCurrentRoomRuntime(DungeonInstance instance, Party party) {
        if (instance == null || instance.getProgress() == null || party == null) return;
        RoomInstance roomInstance = instance.getProgress().getCurrentRoom();
        if (roomInstance == null) return;
        Room roomTemplate = roomManager.get(roomInstance.getRoomId());
        if (roomTemplate == null) return;
        setupRoomRuntime(roomTemplate, roomInstance, party);
    }

    public void setupRoomRuntime(Room roomTemplate, RoomInstance roomInstance, Party party) {
        prepareObjectives(roomTemplate, roomInstance, party);
        prepareRoomEvents(roomTemplate, roomInstance, party);
        if (roomInstance.getActiveObjectives() == null || roomInstance.getActiveObjectives().isEmpty()) {
            roomInstance.setCompleted(true);
        }
    }

    public void triggerRoomStart(RoomInstance roomInstance) {
        if (roomInstance == null || roomInstance.getActiveRoomEvents() == null) return;
        roomInstance.getActiveRoomEvents().forEach(RoomEvent::onRoomStart);
    }

    public void triggerRoomTick(RoomInstance roomInstance) {
        if (roomInstance == null || roomInstance.getActiveRoomEvents() == null) return;
        roomInstance.getActiveRoomEvents().forEach(RoomEvent::onRoomPlay);
    }

    public void triggerRoomEnd(RoomInstance roomInstance) {
        if (roomInstance == null || roomInstance.getActiveRoomEvents() == null) return;
        roomInstance.getActiveRoomEvents().forEach(RoomEvent::onRoomEnd);
    }

    private void prepareObjectives(Room roomTemplate, RoomInstance roomInstance, Party party) {
        List<IObjective> objectives = roomInstance.getActiveObjectives();
        if (objectives == null || objectives.isEmpty()) {
            objectives = new ArrayList<>();
            for (ObjectiveConfig config : roomTemplate.getObjectives()) {
                objectives.add(ObjectiveFactory.create(config));
            }
            roomInstance.setActiveObjectives(objectives);
        }

        for (IObjective objective : objectives) {
            if (objective instanceof BaseObjective base) {
                base.setCallback(o -> {
                    DungeonInstance instance = instanceManager.get(party.getInstanceId());
                    if (instance == null) return;
                    int gained = base.getScore();
                    instance.getProgress().setScore(instance.getProgress().getScore() + gained);

                    if (base.getCompletionScope() == CompletionScope.DUNGEON) {
                        completionService.checkDungeonCompletion(instance, party);
                    } else {
                        completionService.checkRoomCompletion(instance, roomInstance, party);
                    }
                });
            }
        }
    }

    private void prepareRoomEvents(Room roomTemplate, RoomInstance roomInstance, Party party) {
        List<RoomEvent> events = roomInstance.getActiveRoomEvents();
        if (events == null || events.isEmpty()) {
            events = new ArrayList<>();
            for (RoomEventConfig config : roomTemplate.getRoomEvents()) {
                events.add(RoomEventFactory.create(config));
            }
            roomInstance.setActiveRoomEvents(events);
        }

        for (RoomEvent event : events) {
            if (event instanceof BaseRoomEvent base) {
                base.setContext(new RoomEventContext(roomInstance, party, partyService));
                base.setCallback((roomEvent, phase) -> {
                    /*Hook if needed*/
                });
            }
        }
    }
}
