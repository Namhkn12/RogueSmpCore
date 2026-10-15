package com.roguesmp.spawner;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.Keys;
import com.roguesmp.region.SmpRegionFlags;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import org.bukkit.Location;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class SpawnerSystem {

    private static SpawnerSystem INSTANCE;

    private final SpawnerInstanceManager instances;
    private final SpawnerEventListener listener;

    private SpawnerSystem(RogueSmpCore plugin) {
        this.instances = new SpawnerInstanceManager();
        this.listener = new SpawnerEventListener(new SpawnerEventController(instances, plugin));
    }

    public static void init(RogueSmpCore plugin) {
        INSTANCE = new SpawnerSystem(plugin);
    }

    public static SpawnerSystem getInstance() {
        return INSTANCE;
    }

    public SpawnerEventListener listener() {
        return listener;
    }

    public static boolean isActive(Location location) {
        var query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        return query.testState(BukkitAdapter.adapt(location), (LocalPlayer) null, SmpRegionFlags.SPAWNERS);
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
