package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.data.runtime.DungeonInstance;
import com.roguesmp.dungeon.data.runtime.Party;
import com.roguesmp.dungeon.data.runtime.session.DungeonProgress;
import com.roguesmp.dungeon.manager.InstanceManager;
import com.roguesmp.dungeon.service.IPartyService;

public class DungeonTimerService {

    private final InstanceManager instanceManager;
    private final IPartyService partyService;
    private final RoomRuntimeService roomRuntimeService;
    private final DungeonLifecycleService lifecycleService;

    public DungeonTimerService(InstanceManager instanceManager, IPartyService partyService, RoomRuntimeService roomRuntimeService, DungeonLifecycleService lifecycleService) {
        this.instanceManager = instanceManager;
        this.partyService = partyService;
        this.roomRuntimeService = roomRuntimeService;
        this.lifecycleService = lifecycleService;
    }

    public void handleDungeonTimerTick() {
        for (DungeonInstance instance : instanceManager.getAll()) {
            if (instance == null || instance.getTimer() == null
                    || instance.getProgress() == null) continue;

            DungeonProgress.Status status = instance.getProgress().getStatus();
            if (status == DungeonProgress.Status.COMPLETED) continue;

            Party party = partyService.getPartyById(instance.getSession().getPartyId());
            if (party != null) roomRuntimeService.bindCurrentRoomRuntime(instance, party);

            roomRuntimeService.triggerRoomTick(instance.getProgress().getCurrentRoom());

            if (instance.getTimer().isExpired()) {
                lifecycleService.onDungeonTimeExpired(instance);
            }
        }
    }
}