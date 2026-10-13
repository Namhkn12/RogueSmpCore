package com.roguesmp.dungeon.manager;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.data.definition.objective.PersistableObjective;
import com.roguesmp.dungeon.data.definition.room.roomevent.PersistableRoomEvent;
import com.roguesmp.dungeon.data.runtime.DungeonInstance;
import com.roguesmp.dungeon.data.runtime.RoomInstance;
import com.roguesmp.dungeon.data.runtime.session.DungeonProgress;
import com.roguesmp.dungeon.repository.IInstanceRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InstanceManager {

    private final Map<String, DungeonInstance> instances = new LinkedHashMap<>();

    private final IInstanceRepository instanceRepository;

    public InstanceManager(IInstanceRepository instanceRepository) {
        this.instanceRepository = instanceRepository;

        loadAll();
    }

    public void loadAll(){
        instances.clear();
        instanceRepository.loadAll().forEach(instance -> {
            if (instance == null || instance.getSession() == null) return;
            String sessionId = instance.getSession().getSessionId();
            if (sessionId == null || sessionId.isBlank()) return;
            instances.put(sessionId, instance);
        });
    }

    public void saveAll() {
        instances.forEach((uuid, instance) -> {
            snapshotRoomRuntime(instance);
            instanceRepository.save(instance);
        });
    }

    private void snapshotRoomRuntime(DungeonInstance instance) {
        DungeonProgress progress = instance.getProgress();
        if (progress == null) return;

        RoomInstance currentRoom = progress.getCurrentRoom();
        if (currentRoom == null) return;

        if (currentRoom.getActiveObjectives() != null) {
            List<Map<String, Object>> objectiveStates = currentRoom.getActiveObjectives()
                    .stream()
                    .filter(PersistableObjective.class::isInstance)
                    .map(obj -> ((PersistableObjective) obj).serialize())
                    .toList();
            currentRoom.setObjectiveStates(objectiveStates);
        }

        if (currentRoom.getActiveRoomEvents() != null) {
            List<Map<String, Object>> eventStates = currentRoom.getActiveRoomEvents()
                    .stream()
                    .filter(PersistableRoomEvent.class::isInstance)
                    .map(event -> ((PersistableRoomEvent) event).serialize())
                    .toList();
            currentRoom.setRoomEventStates(eventStates);
        }
    }

    public void add(DungeonInstance instance){
        if (instance == null || instance.getSession() == null) return;
        String sessionId = instance.getSession().getSessionId();
        if (sessionId == null || sessionId.isBlank()) return;
        instances.put(sessionId, instance);
        snapshotRoomRuntime(instance);
        instanceRepository.save(instance);
    }

    public DungeonInstance get(String ssid){
        return instances.get(ssid);
    }

    public DungeonInstance remove(String ssid){
        RogueSmpCore.LOGGER.info("Remove dungeon instance with id: {}", ssid);
        instanceRepository.delete(ssid);
        return instances.remove(ssid);
    }

    public List<DungeonInstance> getAll() {
        return List.copyOf(instances.values());
    }
}
