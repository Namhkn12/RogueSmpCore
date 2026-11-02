package com.roguesmp.spawner;


import com.roguesmp.constant.Keys;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.registry.Registries;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

public class SpawnerEventController {

    private final SpawnerInstanceManager spawnerInstanceManager;
    private final Plugin plugin;

    public SpawnerEventController(SpawnerInstanceManager spawnerInstanceManager, Plugin plugin) {
        this.spawnerInstanceManager = spawnerInstanceManager;
        this.plugin = plugin;
    }

    public boolean canBreak(Block block, Player player) {
        String iid = instanceIdOf(block);
        if (iid == null) return true;
        SpawnerInstance instance = spawnerInstanceManager.get(iid);
        return instance == null || instance.runBreak(player, block.getLocation());
    }

    public void handleSpawnerBroken(Block block) {
        String iid = instanceIdOf(block);
        if (iid != null) spawnerInstanceManager.remove(iid);
        SpawnerSystem.markersAt(block.getLocation()).forEach(Entity::remove);
    }

    private static String instanceIdOf(Block block) {
        if (!(block.getState(false) instanceof CreatureSpawner spawner)) return null;
        return spawner.getPersistentDataContainer().get(Keys.SPAWNER_IID, PersistentDataType.STRING);
    }

    public void handleSpawnerAppear(Marker marker) {
        PersistentDataContainer pdc = marker.getPersistentDataContainer();
        if (!pdc.has(Keys.SPAWNER_TID, PersistentDataType.STRING)) return;
        String sid = pdc.get(Keys.SPAWNER_TID, PersistentDataType.STRING);
        if (sid == null || sid.isEmpty()) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!marker.isValid()) return;
            Block block = marker.getLocation().getBlock();
            if (block.getType() != Material.SPAWNER) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> retryRegisterSpawner(marker, sid), 10L);
                return;
            }
            registerSpawner(block, sid);
        }, 5L);
    }

    public void handlePlaceSpawner(Block block, String sid){
        World world = block.getWorld();
        Location loc = block.getLocation().clone().add(0.5, 0, 0.5);

        /*Spawn a marker at the middle of spawner block*/
        world.spawn(loc, Marker.class, entity -> {
            entity.setPersistent(true);
            entity.setInvulnerable(true);
            entity.getPersistentDataContainer().set(Keys.SPAWNER_TID, PersistentDataType.STRING, sid);
        });
    }

    /*Helper*/
    private void registerSpawner(Block block, String templateId) {
        Spawner template = Registries.SPAWNER.get(templateId);
        if (template == null) {
            RogueSmpCore.LOGGER.warn("Spawner template not found: {} at {}", templateId, block.getLocation());
            return;
        }

        CreatureSpawner creatureSpawner = (CreatureSpawner) block.getState(false);
        PersistentDataContainer spawnerPdc = creatureSpawner.getPersistentDataContainer();

        String iid;
        if (spawnerPdc.has(Keys.SPAWNER_IID, PersistentDataType.STRING)) {
            iid = spawnerPdc.get(Keys.SPAWNER_IID, PersistentDataType.STRING);
            if (spawnerInstanceManager.get(iid) != null) return;
        } else {
            iid = UUID.randomUUID().toString();
        }

        Location location = block.getLocation();

        block.setType(Material.AIR, false);
        block.setType(Material.SPAWNER, false);

        CreatureSpawner newSpawner = (CreatureSpawner) location.getBlock().getState();

        PersistentDataContainer newPdc = newSpawner.getPersistentDataContainer();
        newPdc.set(Keys.SPAWNER_IID, PersistentDataType.STRING, iid);
        newSpawner.update();

        spawnerInstanceManager.create(iid, template, location);
        template.applyTo(newSpawner);
    }

    private void retryRegisterSpawner(Marker marker, String templateId) {
        if (!marker.isValid()) return;
        Block blockBelow = marker.getLocation().getBlock();
        if (blockBelow.getType() != Material.SPAWNER) {
            marker.remove();
            return;
        }
        registerSpawner(blockBelow, templateId);
    }
}
