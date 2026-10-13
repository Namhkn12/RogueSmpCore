package com.roguesmp.dungeon_spawner.controller;


import com.roguesmp.constant.Keys;
import com.roguesmp.world.DungeonWorldManager;
import com.roguesmp.RogueSmpCore;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import com.roguesmp.dungeon_spawner.data.SpawnerInstance;
import com.roguesmp.dungeon_spawner.manager.SpawnerInstanceManager;
import com.roguesmp.dungeon_spawner.manager.SpawnerManager;
import com.roguesmp.dungeon_spawner.data.Spawner;
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

    private final SpawnerManager spawnerManager;
    private final SpawnerInstanceManager spawnerInstanceManager;
    private final Plugin plugin;

    public SpawnerEventController(SpawnerManager spawnerManager, SpawnerInstanceManager spawnerInstanceManager, Plugin plugin) {
        this.spawnerManager = spawnerManager;
        this.spawnerInstanceManager = spawnerInstanceManager;
        this.plugin = plugin;
    }

    public boolean handleSpawnerBreak(Block block, Player player){
        if (!(block.getState() instanceof CreatureSpawner spawner))
            return true;

        PersistentDataContainer pdc = spawner.getPersistentDataContainer();
        if (!pdc.has(Keys.SPAWNER_IID, PersistentDataType.STRING))
            return true;

        String iid = pdc.get(Keys.SPAWNER_IID, PersistentDataType.STRING);
        SpawnerInstance instance = spawnerInstanceManager.get(iid);
        if (instance == null) return true;

        boolean allowed = instance.runBreak(player, block.getLocation());
        if (allowed) {
            spawnerInstanceManager.remove(iid);
        }
        return allowed;
    }

    public boolean handleSpawnerSpawn(CreatureSpawner spawner, Entity entity, List<Player> player){
        PersistentDataContainer pdc = spawner.getPersistentDataContainer();
        if (!pdc.has(Keys.SPAWNER_IID, PersistentDataType.STRING))
            return true;

        String iid = pdc.get(Keys.SPAWNER_IID, PersistentDataType.STRING);
        SpawnerInstance instance = spawnerInstanceManager.get(iid);
        if (instance == null) return true;

        return instance.runSpawn((LivingEntity) entity, player,spawner.getLocation());
    }

    public void handleSpawnerLoad(List<Entity> entities, World world){
        if(!DungeonWorldManager.isAnyDungeonWorld(world)) return;
        for (Entity entity : entities) {
            if (entity.getType() != EntityType.MARKER) continue;

            PersistentDataContainer pdc = entity.getPersistentDataContainer();
            if (!pdc.has(Keys.SPAWNER_TID, PersistentDataType.STRING)) continue;

            String sid = pdc.get(Keys.SPAWNER_TID, PersistentDataType.STRING);
            Block blockBelow = entity.getLocation().getBlock();

            if (blockBelow.getType() != Material.SPAWNER) {
                entity.remove();
                continue;
            }

            registerSpawner(blockBelow, sid);
        }
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
        CreatureSpawner creatureSpawner = (CreatureSpawner) block.getState();
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

        Spawner template = spawnerManager.getById(templateId);
        if (template == null) {
            RogueSmpCore.LOGGER.error("Spawner template not found: {}", templateId);
            return;
        }
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
