package com.roguesmp.dungeon.service;

import com.roguesmp.dungeon.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_party.Party;
import com.roguesmp.dungeon.data.runtime.session.DungeonProgress;
import com.roguesmp.dungeon.manager.InstanceManager;
import com.roguesmp.dungeon_party.PartyManager;

public class DungeonTimerService {

    private final InstanceManager instanceManager;
    private final PartyManager partyManager;
    private final RoomRuntimeService roomRuntimeService;
    private final DungeonLifecycleService lifecycleService;

    public DungeonTimerService(InstanceManager instanceManager, PartyManager partyManager, RoomRuntimeService roomRuntimeService, DungeonLifecycleService lifecycleService) {
        this.instanceManager = instanceManager;
        this.partyManager = partyManager;
        this.roomRuntimeService = roomRuntimeService;
        this.lifecycleService = lifecycleService;
    }

    public void handleDungeonTimerTick() {
        for (DungeonInstance instance : instanceManager.getAll()) {
            if (instance == null || instance.getTimer() == null
                    || instance.getProgress() == null) continue;

            DungeonProgress.Status status = instance.getProgress().getStatus();
            if (status == DungeonProgress.Status.COMPLETED) continue;

            Party party = partyManager.getPartyById(instance.getSession().getPartyId());
            if (party != null) roomRuntimeService.bindCurrentRoomRuntime(instance, party);

            roomRuntimeService.triggerRoomTick(instance.getProgress().getCurrentRoom());

            if (instance.getTimer().isExpired()) {
                lifecycleService.onDungeonTimeExpired(instance);
            }
        }
    }
}