package com.roguesmp.spawner;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.Keys;
import com.roguesmp.spawner.behavior.BaseBehavior;
import com.roguesmp.spawner.behavior.BehaviorFactory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpawnerInstanceManager {

    private final Map<String, SpawnerInstance> cache = new HashMap<>();

    public void remove(String siid) {
        cache.remove(siid);
    }

    public void tickAll(double viewRange, int period) {
        for (SpawnerInstance instance : List.copyOf(cache.values())) {
            Location location = instance.getLocation();
            if (!isChunkLoaded(location)) continue;
            if (!holdsInstance(location, instance.getId())) {
                cache.remove(instance.getId());
                SpawnerSystem.markersAt(location).forEach(Entity::remove);
                continue;
            }
            List<Player> viewers = List.copyOf(location.getNearbyPlayers(viewRange));
            if (viewers.isEmpty()) continue;
            try {
                instance.tick(period, viewers);
            } catch (RuntimeException e) {
                RogueSmpCore.LOGGER.error("Spawner {} ({}) failed to tick", instance.getId(), instance.getTemplateId(), e);
            }
        }
    }

    public static boolean isLoadedSpawner(Location location) {
        return isChunkLoaded(location) && location.getBlock().getType() == Material.SPAWNER;
    }

    private static boolean isChunkLoaded(Location location) {
        World world = location.getWorld();
        return world != null && world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    private static boolean holdsInstance(Location location, String iid) {
        if (!(location.getBlock().getState(false) instanceof CreatureSpawner spawner)) return false;
        return iid.equals(spawner.getPersistentDataContainer().get(Keys.SPAWNER_IID, PersistentDataType.STRING));
    }

    public List<SpawnerInstance> getByTemplate(String templateId) {
        return cache.values().stream().filter(instance -> instance.getTemplateId().equals(templateId)).toList();
    }

    public SpawnerInstance get(String siid) {
        return cache.get(siid);
    }

    public SpawnerInstance create(String siid, Spawner spawner, Location location) {
        List<BaseBehavior> behaviors = spawner.getBehaviors().stream()
                .map(BehaviorFactory::create)
                .toList();

        SpawnerInstance instance = new SpawnerInstance(siid, spawner, location, behaviors);
        cache.put(siid, instance);
        return instance;
    }
}
