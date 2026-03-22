package com.roguesmp.dungeon.manager;

import com.roguesmp.dungeon.behavior.BehaviorFactory;
import com.roguesmp.dungeon.behavior.IBehavior;
import com.roguesmp.dungeon.data.Spawner;
import com.roguesmp.dungeon.instance.SpawnerInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SpawnerInstanceManager {

    /**
     * key : marker entity id
     * value : spawner instance data
     * */
    private final Map<String, SpawnerInstance> spawnerInstances = new HashMap<>();

    public SpawnerInstanceManager() {
    }

    public void addInstance(SpawnerInstance instance){
        spawnerInstances.put(instance.getId(), instance);
    }

    public SpawnerInstance getInstance(String id){
        return spawnerInstances.get(id);
    }

    public SpawnerInstance createInstance(String markerId, Spawner spawner){
        SpawnerInstance instance = new SpawnerInstance();
        instance.setId(markerId);
        instance.setTemplateId(spawner.getId());
        List<IBehavior> behaviors = spawner.getBehaviors().stream()
                .map(BehaviorFactory::create)
                .collect(Collectors.toList());

        instance.setBehaviors(behaviors);
        return instance;
    }

    public boolean removeInstance(String id){
        return spawnerInstances.remove(id) != null;
    }
}
