package com.roguesmp.spawner;

import com.roguesmp.spawner.behavior.BaseBehavior;
import com.roguesmp.spawner.behavior.BehaviorFactory;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpawnerInstanceManager {

    private final Map<String, SpawnerInstance> cache = new HashMap<>();

    public void remove(String siid) {
        cache.remove(siid);
    }

    public SpawnerInstance get(String siid) {
        return cache.get(siid);
    }

    public SpawnerInstance create(String siid, Spawner spawner, Location location) {
        List<BaseBehavior> behaviors = spawner.getBehaviors().stream()
                .map(BehaviorFactory::create)
                .toList();

        SpawnerInstance instance = new SpawnerInstance(siid, spawner.getId(), location, behaviors);
        cache.put(siid, instance);
        return instance;
    }
}
