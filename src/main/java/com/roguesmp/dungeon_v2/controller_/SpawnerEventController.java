package com.roguesmp.dungeon_v2.controller_;


import com.roguesmp.dungeon_v2.data.runtime.SpawnerInstance;
import com.roguesmp.dungeon_v2.exception.BaseException;
import com.roguesmp.dungeon_v2.exception.GlobalException;
import com.roguesmp.dungeon_v2.exception.impl.spawner.SpawnerNotFoundException;
import com.roguesmp.dungeon_v2.manager.SpawnerInstanceManager;
import com.roguesmp.dungeon_v2.manager.SpawnerManager;
import com.roguesmp.dungeon_v2.service.ISpawnerService;
import com.roguesmp.dungeon_v2.task.TaskScheduler;
import com.roguesmp.dungeon_v2.utils.DungeonEcho;
import com.roguesmp.dungeon_v2.utils.Log4Craft_;
import com.roguesmp.dungeon_v2.utils.NameSpaceKeys;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

public class SpawnerEventController {

    private final ISpawnerService spawnerService;
    private final SpawnerManager spawnerManager;
    private final SpawnerInstanceManager spawnerInstanceManager;
    private final TaskScheduler taskScheduler;
    private final Log4Craft_ logger;

    public SpawnerEventController(ISpawnerService spawnerService, SpawnerManager spawnerManager, SpawnerInstanceManager spawnerInstanceManager, TaskScheduler taskScheduler, Log4Craft_ logger) {
        this.spawnerService = spawnerService;
        this.spawnerManager = spawnerManager;
        this.spawnerInstanceManager = spawnerInstanceManager;
        this.taskScheduler = taskScheduler;
        this.logger = logger;
    }

    public boolean handleSpawnerBreak(Block block, Player player){
        if (!(block.getState() instanceof CreatureSpawner spawner))
            return true;

        PersistentDataContainer pdc = spawner.getPersistentDataContainer();
        if (!pdc.has(NameSpaceKeys.SPAWNER_IID_KEY, PersistentDataType.STRING))
            return true;

        String iid = pdc.get(NameSpaceKeys.SPAWNER_IID_KEY, PersistentDataType.STRING);
        try {
            SpawnerInstance instance = spawnerInstanceManager.get(iid);
            if (instance == null) return true;

            boolean allowed = instance.getPipeline().runBreak(player, block.getLocation());
            if (allowed) {
                spawnerInstanceManager.remove(iid);
            }
            return allowed;
        } catch (SpawnerNotFoundException e) {
            return true;
        } catch (BaseException e) {
            GlobalException.handle(e);
            return true;
        }
    }

    public void handleSpawnerLoad(List<Entity> entities, String worldName){
        if(!worldName.startsWith("dungeon_")) return;
        for (Entity entity : entities) {
            if (entity.getType() != EntityType.MARKER) continue;

            PersistentDataContainer pdc = entity.getPersistentDataContainer();
            if (!pdc.has(NameSpaceKeys.SPAWNER_TID_KEY, PersistentDataType.STRING)) continue;

            try {
                String sid = pdc.get(NameSpaceKeys.SPAWNER_TID_KEY, PersistentDataType.STRING);
                Block blockBelow = entity.getLocation().getBlock();

                if (blockBelow.getType() != Material.SPAWNER) {
                    entity.remove();
                    continue;
                }

                registerSpawner(blockBelow, sid);
            } catch (BaseException e) {
                GlobalException.handle(e);
            }
        }
    }

    public void handleSpawnerAppear(Marker marker) {
        PersistentDataContainer pdc = marker.getPersistentDataContainer();
        if (!pdc.has(NameSpaceKeys.SPAWNER_TID_KEY, PersistentDataType.STRING)) return;
        String sid = pdc.get(NameSpaceKeys.SPAWNER_TID_KEY, PersistentDataType.STRING);
        if (sid == null || sid.isEmpty()) return;

        taskScheduler.runLater(5L, () -> {
            if (!marker.isValid()) return;
            Block block = marker.getLocation().getBlock();
            if (block.getType() != Material.SPAWNER) {
                taskScheduler.runLater(10L, () -> retryRegisterSpawner(marker, sid));
                return;
            }
            registerSpawner(block, sid);
        });
    }

    public void handleSpawnerSpawn(){

    }

    /*Admin api*/
    public void handleGetSpawner(){

    }

    public void handlePlaceSpawner(Block block, String sid){
        World world = block.getWorld();
        Location loc = block.getLocation().clone().add(0.5, 0, 0.5);

        /*Spawn a marker at the middle of spawner block*/
        world.spawn(loc, Marker.class, entity -> {
            entity.setPersistent(true);
            entity.setInvulnerable(true);
            entity.getPersistentDataContainer().set(NameSpaceKeys.SPAWNER_TID_KEY, PersistentDataType.STRING, sid);
        });
    }

    /*Helper*/
    private void registerSpawner(Block block, String templateId) {
        try {
            CreatureSpawner creatureSpawner = (CreatureSpawner) block.getState();
            PersistentDataContainer spawnerPdc = creatureSpawner.getPersistentDataContainer();

            String iid;
            if (spawnerPdc.has(NameSpaceKeys.SPAWNER_IID_KEY, PersistentDataType.STRING)) {
                iid = spawnerPdc.get(NameSpaceKeys.SPAWNER_IID_KEY, PersistentDataType.STRING);
                if (spawnerService.getInstance(iid) != null) return;
            } else {
                iid = UUID.randomUUID().toString();
                spawnerPdc.set(NameSpaceKeys.SPAWNER_IID_KEY, PersistentDataType.STRING, iid);
                creatureSpawner.update();
            }

            spawnerService.createInstance(templateId, iid, block.getLocation());
            spawnerService.applyTemplate(templateId, creatureSpawner);
        } catch (BaseException e) {
            GlobalException.handle(e);
        }
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
