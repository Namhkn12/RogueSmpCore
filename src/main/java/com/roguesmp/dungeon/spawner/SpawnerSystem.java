package com.roguesmp.dungeon.spawner;

import com.google.gson.GsonBuilder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.Keys;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class SpawnerSystem {

    private static SpawnerSystem INSTANCE;

    private final SpawnerManager templates;
    private final SpawnerInstanceManager instances;
    private final SpawnerEventListener listener;

    private SpawnerSystem(RogueSmpCore plugin) {
        this.templates = new SpawnerManager(new SpawnerRepository(plugin, new GsonBuilder().setPrettyPrinting().create()));
        this.instances = new SpawnerInstanceManager();
        this.listener = new SpawnerEventListener(new SpawnerEventController(templates, instances, plugin));
    }

    public static void init(RogueSmpCore plugin) {
        INSTANCE = new SpawnerSystem(plugin);
    }

    public static SpawnerSystem getInstance() {
        return INSTANCE;
    }

    public SpawnerManager templates() {
        return templates;
    }

    public SpawnerEventListener listener() {
        return listener;
    }

    public void disarm(CreatureSpawner spawner) {
        PersistentDataContainer pdc = spawner.getPersistentDataContainer();
        String instanceId = pdc.get(Keys.SPAWNER_IID, PersistentDataType.STRING);
        if (instanceId != null) instances.remove(instanceId);

        spawner.setSpawnedType(null);
        spawner.setSpawnCount(0);
        spawner.setMaxNearbyEntities(0);
        spawner.setDelay(-1);
        spawner.getPotentialSpawns().clear();
        pdc.remove(Keys.SPAWNER_IID);
        pdc.remove(Keys.SPAWNER_TID);
        spawner.update(true, false);
    }
}
