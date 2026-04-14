package com.roguesmp.dungeon_v2.manager;

import com.roguesmp.dungeon_v2.data.definition.objective.PersistableObjective;
import com.roguesmp.dungeon_v2.data.definition.room.roomevent.PersistableRoomEvent;
import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.data.runtime.RoomInstance;
import com.roguesmp.dungeon_v2.data.runtime.session.DungeonProgress;
import com.roguesmp.dungeon_v2.repository.IInstanceRepository;
import com.roguesmp.dungeon_v2.utils.Log4Craft_;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InstanceManager {

    private final Map<String, DungeonInstance> instances = new LinkedHashMap<>();

    private final IInstanceRepository instanceRepository;
    private final ScoreBoardManager scoreBoardManager;
    private final Log4Craft_ logger;

    public InstanceManager(IInstanceRepository instanceRepository, ScoreBoardManager scoreBoardManager, Log4Craft_ logger) {
        this.instanceRepository = instanceRepository;
        this.scoreBoardManager = scoreBoardManager;
        this.logger = logger;

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
        logger.info(this.getClass(), "Remove dungeon instance with id: " + ssid);
        instanceRepository.delete(ssid);
        return instances.remove(ssid);
    }

    public List<DungeonInstance> getAll() {
        return List.copyOf(instances.values());
    }
}
