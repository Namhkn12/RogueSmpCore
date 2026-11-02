package com.roguesmp.spawner;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.Keys;
import com.roguesmp.region.SmpRegionFlags;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import org.bukkit.Location;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Marker;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class SpawnerSystem {

    private static final int TICK_PERIOD = 5;
    private static final double VIEW_RANGE = 48;

    private static SpawnerSystem INSTANCE;

    private final SpawnerInstanceManager instances;
    private final SpawnerEventListener listener;

    private SpawnerSystem(RogueSmpCore plugin) {
        this.instances = new SpawnerInstanceManager();
        this.listener = new SpawnerEventListener(new SpawnerEventController(instances, plugin));
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> instances.tickAll(VIEW_RANGE, TICK_PERIOD), TICK_PERIOD, TICK_PERIOD);
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

    public void refreshTemplate(String previousId, Spawner template) {
        for (SpawnerInstance instance : instances.getByTemplate(previousId)) {
            Location location = instance.getLocation();
            if (!SpawnerInstanceManager.isLoadedSpawner(location) || !(location.getBlock().getState(false) instanceof CreatureSpawner spawner)) {
                instances.remove(instance.getId());
                continue;
            }
            if (!previousId.equals(template.getId())) {
                markersAt(location).forEach(marker -> marker.getPersistentDataContainer().set(Keys.SPAWNER_TID, PersistentDataType.STRING, template.getId()));
            }
            instances.create(instance.getId(), template, location);
            template.applyTo(spawner);
        }
    }

    public void deleteTemplate(String templateId) {
        for (SpawnerInstance instance : instances.getByTemplate(templateId)) {
            instances.remove(instance.getId());
            Location location = instance.getLocation();
            if (!SpawnerInstanceManager.isLoadedSpawner(location)) continue;
            markersAt(location).forEach(Entity::remove);
            if (location.getBlock().getState(false) instanceof CreatureSpawner spawner) disarm(spawner);
        }
    }

    public static List<Marker> markersAt(Location blockLocation) {
        return blockLocation.clone().add(0.5, 0, 0.5).getNearbyEntitiesByType(Marker.class, 0.5).stream()
                .filter(marker -> marker.getPersistentDataContainer().has(Keys.SPAWNER_TID, PersistentDataType.STRING))
                .toList();
    }

    public void disarm(CreatureSpawner spawner) {
        PersistentDataContainer pdc = spawner.getPersistentDataContainer();
        String instanceId = pdc.get(Keys.SPAWNER_IID, PersistentDataType.STRING);
        if (instanceId != null) instances.remove(instanceId);

        spawner.setSpawnedType(null);
        spawner.setSpawnCount(0);
        spawner.setMaxNearbyEntities(0);
        spawner.setDelay(-1);
        spawner.setPotentialSpawns(List.of());
        pdc.remove(Keys.SPAWNER_IID);
        pdc.remove(Keys.SPAWNER_TID);
        spawner.update(true, false);
    }
}
