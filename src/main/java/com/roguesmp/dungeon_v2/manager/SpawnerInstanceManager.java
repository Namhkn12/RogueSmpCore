package com.roguesmp.dungeon_v2.manager;

import com.roguesmp.dungeon_v2.data.definition.spawner.Spawner;
import com.roguesmp.dungeon_v2.data.definition.spawner.behavior.BehaviorFactory;
import com.roguesmp.dungeon_v2.data.definition.spawner.behavior.IBehavior;
import com.roguesmp.dungeon_v2.data.runtime.SpawnerInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpawnerInstanceManager {

    private final Map<String, SpawnerInstance> cache = new HashMap<>();

    public SpawnerInstanceManager() {
    }

    public void add(SpawnerInstance instance) {cache.put(instance.getId(), instance);}

    public void remove(String siid){cache.remove(siid);}

    public SpawnerInstance get(String siid){
        return cache.get(siid);
    }

    public SpawnerInstance create(String siid, Spawner spawner){
        SpawnerInstance instance = new SpawnerInstance();
        instance.setId(siid);
        instance.setTemplateId(spawner.getId());
        List<IBehavior> behaviors = spawner.getBehaviors().stream()
                .map(BehaviorFactory::create)
                .toList();
        instance.setBehaviors(behaviors);
        return instance;
    }
}
