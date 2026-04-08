package com.roguesmp.dungeon_v2.manager;

import com.roguesmp.dungeon_v2.data.definition.objective.PersistableObjective;
import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.data.runtime.RoomInstance;
import com.roguesmp.dungeon_v2.data.runtime.session.DungeonProgress;
import com.roguesmp.dungeon_v2.repository.IInstanceRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InstanceManager {

    private final Map<UUID, DungeonInstance> instances = new LinkedHashMap<>();

    private final IInstanceRepository instanceRepository;
    private final ScoreBoardManager scoreBoardManager;

    public InstanceManager(IInstanceRepository instanceRepository, ScoreBoardManager scoreBoardManager) {
        this.instanceRepository = instanceRepository;
        this.scoreBoardManager = scoreBoardManager;

        loadAll();
    }

    public void loadAll(){
        instances.clear();
        instanceRepository.loadAll().forEach(instance -> instances.put(instance.getSession().getSessionId(), instance));
    }

    public void saveAll() {
        instances.forEach((uuid, instance) -> {
            snapshotObjectives(instance);
            instanceRepository.save(instance);
        });
    }

    private void snapshotObjectives(DungeonInstance instance) {
        DungeonProgress progress = instance.getProgress();
        if (progress == null) return;

        RoomInstance currentRoom = progress.getCurrentRoom();
        if (currentRoom == null || currentRoom.getActiveObjectives() == null) return;

        List<Map<String, Object>> states = currentRoom.getActiveObjectives()
                .stream()
                .map(obj -> ((PersistableObjective) obj).serialize())
                .toList();
        currentRoom.setObjectiveStates(states);
    }

    public void add(DungeonInstance instance){
        instances.put(instance.getSession().getSessionId(), instance);
        instanceRepository.save(instance);
    }

    public DungeonInstance get(UUID ssid){
        return instances.get(ssid);
    }

    public DungeonInstance remove(UUID ssid){
        return instances.remove(ssid);
    }

    public List<DungeonInstance> getAll() {
        return List.copyOf(instances.values());
    }
}
